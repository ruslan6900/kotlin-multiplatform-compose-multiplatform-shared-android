#include <QCoreApplication>
#include <QDateTime>
#include <QDBusConnection>
#include <QDBusMessage>
#include <QDir>
#include <QFile>
#include <QFileInfo>
#include <QTextStream>
#include <QTimer>
#include <RuntimeManager/RuntimeDispatcher>
#include <RuntimeManager/Task>
#include <cstdio>

static QString logFile() {
    return QDir::homePath() + "/.local/share/com.example/aurorakmpdemo/system-events.log";
}
static void logLine(const QString &event, const QString &details) {
    const QString text = QStringLiteral("[%1][pid=%2][%3] %4")
        .arg(QDateTime::currentDateTimeUtc().toString("yyyy-MM-ddTHH:mm:ss.zzzZ"))
        .arg(QCoreApplication::applicationPid()).arg(event, details);
    QDir().mkpath(QFileInfo(logFile()).absolutePath());
    QFile file(logFile());
    if (file.open(QIODevice::Append | QIODevice::Text)) {
        QTextStream stream(&file); stream.setCodec("UTF-8"); stream << text << '\n';
    }
    fprintf(stderr, "%s\n", text.toUtf8().constData());
}
static bool notify() {
    QDBusMessage call = QDBusMessage::createMethodCall("org.freedesktop.Notifications", "/org/freedesktop/Notifications", "org.freedesktop.Notifications", "Notify");
    QVariantMap hints;
    hints.insert("x-nemo-preview-summary", QString::fromUtf8("Периодическое уведомление"));
    hints.insert("x-nemo-preview-body", QString::fromUtf8("Проверка фоновой задачи Aurora"));
    call << QString("Aurora KMP Demo") << uint(0) << QString("com.example.aurorakmpdemo")
         << QString::fromUtf8("Периодическое уведомление")
         << QString::fromUtf8("Тестовая фоновая задача выполнена.") << QStringList() << hints << int(-1);
    const auto reply = QDBusConnection::sessionBus().call(call, QDBus::Block, 5000);
    if (reply.type() == QDBusMessage::ErrorMessage) {
        logLine("NOTIFY_ERROR", reply.errorName() + ": " + reply.errorMessage()); return false;
    }
    const auto id = reply.arguments().value(0).toUInt();
    logLine("NOTIFY_REPLY", QString("id=%1 accepted=%2").arg(id).arg(id > 0));
    return id > 0;
}
int main(int argc, char **argv) {
    QCoreApplication app(argc, argv);
    const QString taskId = QString::fromUtf8(qgetenv("AURORA_TASK_ID"));
    logLine("PROCESS_START", QString("taskId=%1 appId=%2 args=%3").arg(taskId, QString::fromUtf8(qgetenv("AURORA_APP_ID")), app.arguments().join(' ')));
    if (!taskId.isEmpty()) {
        auto *dispatcher = RuntimeManager::RuntimeDispatcher::instance();
        for (const QString &name : {QString("DemoWorker"), QString("DemoPeriodicWork"), QString("DemoPeriodicNotification")}) {
            dispatcher->onTaskStarted(name, [&app](const QString &id) {
                logLine("TASK_START", QString("id=%1").arg(id));
                bool ok = true;
                if (id == "DemoPeriodicNotification") ok = notify();
                logLine("TASK_END", QString("id=%1 success=%2").arg(id).arg(ok));
                QTimer::singleShot(0, &app, [&app, ok]() { app.exit(ok ? 0 : 1); });
            });
        }
        QTimer::singleShot(15000, &app, [&app]() { logLine("TASK_TIMEOUT", "Task callback was not delivered"); app.exit(2); });
        return app.exec();
    }
    const QString cmd = app.arguments().value(1);
    const bool stop = cmd.startsWith("stop-");
    const QString kind = stop ? cmd.mid(5) : cmd;
    QString id;
    if (kind == "worker") id = "DemoWorker";
    else if (kind == "periodic-work") id = "DemoPeriodicWork";
    else if (kind == "periodic-notification") id = "DemoPeriodicNotification";
    else { logLine("INVALID_COMMAND", cmd); return 2; }
    RuntimeManager::Task task(id);
    logLine("REQUEST", QString("action=%1 id=%2 interval=%3 autostart=false").arg(stop ? "stop" : "start", id).arg(kind == "worker" ? 0 : 900));
    const auto callback = [&app, id, stop](const RuntimeManager::Error &error) {
        if (error) {
            logLine("REQUEST_ERROR", QString("id=%1 code=%2 message=%3").arg(id).arg(error.code()).arg(error.message()));
            fprintf(stdout, "ERROR: %s\n", error.message().toUtf8().constData());
            app.exit(1);
        } else {
            logLine("REQUEST_ACCEPTED", QString("id=%1 action=%2").arg(id, stop ? "stop" : "start"));
            fprintf(stdout, "OK\n"); app.exit(0);
        }
    };
    if (stop) task.stop(RuntimeManager::Task::StopFlag::Disable, callback);
    else {
        task.withMaximumRunningTime(60);
        if (kind != "worker") task.withInterval(900).withAutostart(false);
        task.start(callback);
    }
    QTimer::singleShot(10000, &app, [&app]() { logLine("REQUEST_TIMEOUT", "RuntimeManager did not reply"); app.exit(2); });
    return app.exec();
}
