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
 * Development-only privileged adapter. It deliberately exposes no arbitrary
 * ConnMan configuration: only one harmless split-tunnel test provider can be
 * created or controlled. A release app should call ConnMan directly from a
 * VPN-permitted Sailjail context instead.
 */
class VpnBridge final : public QObject {
    Q_OBJECT
    Q_CLASSINFO("D-Bus Interface", "com.example.aurorakmpdemo.VpnBridge")

public slots:
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

        // ConnMan returns an error for an existing profile. Its object path is
        // deterministic for this fixed Host/Domain pair, so it is safe to reuse.
        if (reply.error().name().contains(QStringLiteral("AlreadyExists"), Qt::CaseInsensitive) ||
            reply.error().message().contains(QStringLiteral("already"), Qt::CaseInsensitive)) {
            return ok(QString::fromLatin1(kTestPath));
        }
        return fail(QStringLiteral("create:%1:%2").arg(reply.error().name(), reply.error().message()));
    }

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

    QString DisconnectTestProfile() {
        QDBusInterface connection(kConnmanService, kTestPath, kConnectionInterface, QDBusConnection::systemBus());
        if (!connection.isValid()) return fail(QStringLiteral("test-profile-not-found"));
        QDBusMessage reply = connection.call(QStringLiteral("Disconnect"));
        if (reply.type() == QDBusMessage::ErrorMessage) {
            return fail(QStringLiteral("disconnect:%1:%2").arg(reply.errorName(), reply.errorMessage()));
        }
        return ok(QStringLiteral("disconnect-requested"));
    }

    QString TestProfileState() {
        QDBusInterface connection(kConnmanService, kTestPath, kConnectionInterface, QDBusConnection::systemBus());
        if (!connection.isValid()) return fail(QStringLiteral("test-profile-not-found"));
        QDBusReply<QVariantMap> reply = connection.call(QStringLiteral("GetProperties"));
        if (!reply.isValid()) return fail(QStringLiteral("properties:%1:%2").arg(reply.error().name(), reply.error().message()));
        return ok(reply.value().value(QStringLiteral("State")).toString());
    }

private:
    static bool isSafeProfileName(const QString &name) {
        return !name.isEmpty() && name.size() <= 64 &&
            QRegularExpression(QStringLiteral("^[A-Za-z0-9._ -]+$")).match(name).hasMatch();
    }
};

int main(int argc, char *argv[]) {
    QCoreApplication app(argc, argv);
    QDBusConnection bus = QDBusConnection::systemBus();
    if (!bus.registerService(QString::fromLatin1(kBridgeService))) return 2;
    VpnBridge bridge;
    if (!bus.registerObject(QString::fromLatin1(kBridgePath), &bridge, QDBusConnection::ExportAllSlots)) return 3;
    return app.exec();
}

#include "main.moc"
