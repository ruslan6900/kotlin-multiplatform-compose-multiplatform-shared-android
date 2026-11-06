TEMPLATE = lib
TARGET = aurora-test-route-proto-plugin
QMAKE_PREFIX_SHLIB =

# ConnMan loads this module and resolves its API symbols itself.  The plugin
# must therefore export only connman_plugin_desc.
CONFIG += plugin hide_symbols link_pkgconfig c99
# connman.pc carries libtool-only flags (-module/-avoid-version) that this
# qmake invocation forwards straight to g++. ConnMan symbols are resolved by
# the daemon when it loads the module, so only its headers are required here.
PKGCONFIG += glib-2.0 dbus-1

SOURCES += split_tunnel_plugin.c

target.path = /usr/lib/connman/plugins-vpn
INSTALLS += target
