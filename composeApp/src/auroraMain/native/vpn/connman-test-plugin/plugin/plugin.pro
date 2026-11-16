TEMPLATE = lib
TARGET = aurora-test-route-proto-plugin
QMAKE_PREFIX_SHLIB =

# ConnMan сам загружает этот модуль и разрешает его API-символы. Поэтому plugin
# должен экспортировать только connman_plugin_desc.
CONFIG += plugin hide_symbols link_pkgconfig c99
# connman.pc содержит флаги только для libtool (-module/-avoid-version), которые
# этот вызов qmake передаёт прямо в g++. Символы ConnMan разрешает daemon при
# загрузке модуля, поэтому здесь нужны только его headers.
PKGCONFIG += glib-2.0 dbus-1

SOURCES += split_tunnel_plugin.c

target.path = /usr/lib/connman/plugins-vpn
INSTALLS += target
