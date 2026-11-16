TEMPLATE = app
TARGET = com.example.aurorakmpdemo.test-route-vpn
CONFIG += console c99 link_pkgconfig
PKGCONFIG += dbus-1

SOURCES += test_route_provider.c

target.path = /usr/bin
INSTALLS += target

vpnConfig.path = /etc/connman/vpn-plugin
vpnConfig.files = aurora-test-route.conf
INSTALLS += vpnConfig
