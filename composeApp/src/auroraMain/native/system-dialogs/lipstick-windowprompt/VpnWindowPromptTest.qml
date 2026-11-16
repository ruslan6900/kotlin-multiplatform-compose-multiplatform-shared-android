import QtQuick 2.6
import Sailfish.Lipstick 1.1
import Nemo.DBus 2.0

SystemDialog {
    id: first
    property var promptConfig: ({})
    property var nextDialog
    signal done(var window, bool unregister)
    function init(config) { promptConfig = config; console.log("VPN_TEST_FIRST_OPEN"); raise(); show() }
    contentHeight: screenHeight
    autoDismiss: true
    onDismissed: { console.log("VPN_TEST_DISMISSED"); done(first, false) }
    DBusInterface { id: runtime; bus: DBus.SessionBus; service: "ru.omp.RuntimeManager"; path: "/ru/omp/RuntimeManager/Control1"; iface: "ru.omp.RuntimeManager.Control1" }
    Item {
        width: parent.width; height: first.screenHeight
        Rectangle {
            anchors.centerIn: parent; width: parent.width - 64; height: 280; radius: 18; color: "#263655"
            Column {
                anchors.fill: parent; anchors.margins: 24; spacing: 22
                Text { width: parent.width; wrapMode: Text.Wrap; color: "white"; font.pixelSize: 23; text: "VPN после перезагрузки" }
                Text { width: parent.width; wrapMode: Text.Wrap; color: "white"; font.pixelSize: 17; text: "Тест: восстановление подключения к VPN." }
                Rectangle {
                    width: parent.width; height: 60; radius: 8; color: "#526cc2"
                    Text { anchors.centerIn: parent; color: "white"; font.pixelSize: 18; text: "Продолжить" }
                    MouseArea { anchors.fill: parent; onClicked: { console.log("VPN_TEST_FIRST_CLICK"); first.nextDialog = secondComponent.createObject(first); first.nextDialog.activate(); first.lower() } }
                }
            }
        }
    }
    Component {
        id: secondComponent
        SystemDialog {
            id: second
            contentHeight: screenHeight
            onDismissed: { console.log("VPN_TEST_SECOND_DISMISS"); first.done(first, false) }
            Item {
                width: parent.width; height: second.screenHeight
                Rectangle {
                    anchors.centerIn: parent; width: parent.width - 64; height: 280; radius: 18; color: "#263655"
                    Column {
                        anchors.fill: parent; anchors.margins: 24; spacing: 22
                        Text { width: parent.width; wrapMode: Text.Wrap; color: "white"; font.pixelSize: 23; text: "Восстановление VPN" }
                        Text { width: parent.width; wrapMode: Text.Wrap; color: "white"; font.pixelSize: 17; text: "Второе окно: открыть демо-приложение." }
                        Rectangle {
                            width: parent.width; height: 60; radius: 8; color: "#526cc2"
                            Text { anchors.centerIn: parent; color: "white"; font.pixelSize: 18; text: "Открыть приложение" }
                            MouseArea { anchors.fill: parent; onClicked: { console.log("VPN_TEST_OPEN_APP"); runtime.call("Start", ["com.example.aurorakmpdemo"], function() { console.log("VPN_TEST_APP_REQUESTED"); second.dismiss(); first.done(first, false) }, function(e) { console.log("VPN_TEST_APP_ERROR", e); second.dismiss(); first.done(first, false) }) } }
                        }
                    }
                }
            }
        }
    }
}
