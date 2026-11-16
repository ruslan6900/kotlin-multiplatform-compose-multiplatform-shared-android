TEMPLATE = app
TARGET = aurora-demo-tasks
CONFIG += console c++11 link_pkgconfig
CONFIG -= app_bundle
QT += core dbus
PKGCONFIG += runtime-manager-qt5
SOURCES += main.cpp
