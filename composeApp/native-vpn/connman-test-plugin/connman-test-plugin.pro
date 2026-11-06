TEMPLATE = subdirs
CONFIG += ordered

SUBDIRS += plugin provider bridge
plugin.file = plugin/plugin.pro
provider.file = provider/provider.pro
bridge.file = bridge/bridge.pro
