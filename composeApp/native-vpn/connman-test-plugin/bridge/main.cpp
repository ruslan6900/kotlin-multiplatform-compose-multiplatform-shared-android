#include <QCoreApplication>
#include <QDBusConnection>
#include <QDBusError>
#include <QDBusInterface>
#include <QDBusObjectPath>
#include <QDBusReply>
#include <QRegularExpression>
#include <QVariantMap>
#include <QObject>

namespace {
// Эти идентификаторы должны совпадать с PlatformVpnController.aurora.kt и
// D-Bus policy, устанавливаемой рядом с этим исполняемым файлом.
const char *kConnmanService = "net.connman.vpn";
const char *kManagerInterface = "net.connman.vpn.Manager";
const char *kConnectionInterface = "net.connman.vpn.Connection";
const char *kBridgeService = "com.example.aurorakmpdemo.VpnBridge";
const char *kBridgePath = "/com/example/aurorakmpdemo/VpnBridge";
const char *kTestType = "aurora_test_route";
const char *kTestHost = "198.51.100.1";
const char *kTestDomain = "example.test";
const char *kTestPath = "/net/connman/vpn/connection/198_51_100_1_example_test";

QString ok(const QString &value) { return QStringLiteral("ok:") + value; }
QString fail(const QString &value) { return QStringLiteral("error:") + value; }
}

/**
 * Привилегированный адаптер только для development.
 *
 * Это единственный процесс демо, которому разрешено превращать действие UI в
 * вызов ConnMan system bus. Экспортируемый API намеренно узкий: caller может
 * создать/переиспользовать один profile, подключить, отключить или прочитать
 * его состояние. Host, domain, provider type, routes и DNS ниже — константы,
 * а не входные данные caller. Это не даёт UI без sandbox стать общим VPN manager.
 *
 * Production-реализации нужны согласованная D-Bus policy и доверенная
 * system/extended подпись; этот bridge не заменяет ни то, ни другое.
 */
class VpnBridge final : public QObject {
    Q_OBJECT
    Q_CLASSINFO("D-Bus Interface", "com.example.aurorakmpdemo.VpnBridge")

public slots:
    /** Создаёт фиксированный ConnMan profile или возвращает его детерминированный путь. */
    QString EnsureTestProfile(const QString &requestedName) {
        if (!isSafeProfileName(requestedName)) {
            return fail(QStringLiteral("invalid-profile-name"));
        }
        QDBusInterface manager(kConnmanService, "/", kManagerInterface, QDBusConnection::systemBus());
        if (!manager.isValid()) return fail(QStringLiteral("connman-manager-unavailable"));

        QVariantMap properties;
        properties.insert(QStringLiteral("Name"), requestedName);
        properties.insert(QStringLiteral("Type"), QString::fromLatin1(kTestType));
        properties.insert(QStringLiteral("Host"), QString::fromLatin1(kTestHost));
        properties.insert(QStringLiteral("Domain"), QString::fromLatin1(kTestDomain));
        QDBusReply<QDBusObjectPath> reply = manager.call(QStringLiteral("Create"), properties);
        if (reply.isValid()) return ok(reply.value().path());

        // ConnMan возвращает ошибку для существующего profile. Его object path
        // детерминирован для фиксированной пары Host/Domain, поэтому его можно переиспользовать.
        if (reply.error().name().contains(QStringLiteral("AlreadyExists"), Qt::CaseInsensitive) ||
            reply.error().message().contains(QStringLiteral("already"), Qt::CaseInsensitive)) {
            return ok(QString::fromLatin1(kTestPath));
        }
        return fail(QStringLiteral("create:%1:%2").arg(reply.error().name(), reply.error().message()));
    }

    /** Идемпотентный запрос подключения. Фактический успех проверяет TestProfileState. */
    QString ConnectTestProfile() {
        QDBusInterface connection(kConnmanService, kTestPath, kConnectionInterface, QDBusConnection::systemBus());
        if (!connection.isValid()) return fail(QStringLiteral("test-profile-not-found"));
        QDBusReply<QVariantMap> properties = connection.call(QStringLiteral("GetProperties"));
        if (properties.isValid() && properties.value().value(QStringLiteral("State")).toString() == QStringLiteral("ready")) {
            return ok(QStringLiteral("already-ready"));
        }
        QDBusMessage reply = connection.call(QStringLiteral("Connect"));
        if (reply.type() == QDBusMessage::ErrorMessage) {
            return fail(QStringLiteral("connect:%1:%2").arg(reply.errorName(), reply.errorMessage()));
        }
        return ok(QStringLiteral("connect-requested"));
    }

    /** Запрашивает асинхронное отключение ConnMan; Kotlin caller затем опрашивает состояние. */
    QString DisconnectTestProfile() {
        QDBusInterface connection(kConnmanService, kTestPath, kConnectionInterface, QDBusConnection::systemBus());
        if (!connection.isValid()) return fail(QStringLiteral("test-profile-not-found"));
        QDBusMessage reply = connection.call(QStringLiteral("Disconnect"));
        if (reply.type() == QDBusMessage::ErrorMessage) {
            return fail(QStringLiteral("disconnect:%1:%2").arg(reply.errorName(), reply.errorMessage()));
        }
        return ok(QStringLiteral("disconnect-requested"));
    }

    /** Возвращает исходное свойство ConnMan State в протоколе bridge `ok:<state>`. */
    QString TestProfileState() {
        QDBusInterface connection(kConnmanService, kTestPath, kConnectionInterface, QDBusConnection::systemBus());
        if (!connection.isValid()) return fail(QStringLiteral("test-profile-not-found"));
        QDBusReply<QVariantMap> reply = connection.call(QStringLiteral("GetProperties"));
        if (!reply.isValid()) return fail(QStringLiteral("properties:%1:%2").arg(reply.error().name(), reply.error().message()));
        return ok(reply.value().value(QStringLiteral("State")).toString());
    }

private:
    /** Не даёт использовать label profile как канал инъекции в привилегированный сервис. */
    static bool isSafeProfileName(const QString &name) {
        return !name.isEmpty() && name.size() <= 64 &&
            QRegularExpression(QStringLiteral("^[A-Za-z0-9._ -]+$")).match(name).hasMatch();
    }
};

int main(int argc, char *argv[]) {
    // systemd запускает процесс. Регистрация имени и объекта открывает методы
    // только по документированным service/path/interface.
    QCoreApplication app(argc, argv);
    QDBusConnection bus = QDBusConnection::systemBus();
    if (!bus.registerService(QString::fromLatin1(kBridgeService))) return 2;
    VpnBridge bridge;
    if (!bus.registerObject(QString::fromLatin1(kBridgePath), &bridge, QDBusConnection::ExportAllSlots)) return 3;
    return app.exec();
}

#include "main.moc"
