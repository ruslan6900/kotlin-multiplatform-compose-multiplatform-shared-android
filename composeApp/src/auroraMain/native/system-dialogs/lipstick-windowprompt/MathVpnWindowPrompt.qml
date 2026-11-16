import QtQuick 2.6
import Sailfish.Lipstick 1.1
import Nemo.DBus 2.0

SystemDialog {
    id: first
    property var promptConfig: ({})
    property var nextDialog
    property string validationMessage: ""
    property int checkAttempts: 0
    property string pendingAnswer: ""
    property bool checking: false
    signal done(var window, bool unregister)

    function init(config) {
        promptConfig = config
        console.log("MATH_VPN: first dialog shown")
        raise()
        show()
    }
    function checkAnswer() {
        checkAttempts++
        console.log("MATH_VPN: application answer check, attempt", checkAttempts)
        answerService.call("Check", [pendingAnswer], function(accepted) {
            if (!checking) return
            checking = false
            console.log("MATH_VPN: application replied", accepted)
            if (accepted !== true) {
                validationMessage = "Ответ неверный. Попробуйте ещё раз."
                return
            }
            validationMessage = ""
            nextDialog = secondComponent.createObject(first)
            nextDialog.activate()
            first.lower()
        }, function(error) {
            if (!checking) return
            console.log("MATH_VPN: application check failed", error)
            if (checkAttempts < 3 && first.visible) {
                validationMessage = "Ожидаем ответ приложения…"
                retryTimer.restart()
            } else {
                checking = false
                validationMessage = "Не удалось выполнить проверку в приложении. Повторите попытку."
            }
        })
    }
    Timer { id: retryTimer; interval: 1000; onTriggered: first.checkAnswer() }
    contentHeight: screenHeight
    useDialogBackground: false
    color: "transparent"
    onDismissed: { retryTimer.stop(); checking = false; console.log("MATH_VPN: first dialog dismissed"); done(first, false) }

    DBusInterface {
        id: answerService
        bus: DBus.SessionBus
        service: "com.example.aurorakmpdemo"
        path: "/com/example/aurorakmpdemo/DialogAnswer"
        iface: "com.example.aurorakmpdemo.DialogAnswer"
    }
    DBusInterface {
        id: vpnBridge
        bus: DBus.SystemBus
        service: "com.example.aurorakmpdemo.VpnBridge"
        path: "/com/example/aurorakmpdemo/VpnBridge"
        iface: "com.example.aurorakmpdemo.VpnBridge"
    }
    DBusInterface {
        id: vpnConnection
        bus: DBus.SystemBus
        service: "net.connman.vpn"
        path: "/net/connman/vpn/connection/198_51_100_1_example_test"
        iface: "net.connman.vpn.Connection"
    }
    Item {
        width: parent.width; height: first.screenHeight
        Rectangle { anchors.fill: parent; color: "transparent" }
        MouseArea {
            anchors.fill: parent
            onClicked: { console.log("MATH_VPN: outside first dialog"); retryTimer.stop(); first.checking = false; Qt.inputMethod.hide(); first.dismiss(); first.done(first, false) }
        }
        Rectangle {
            anchors.centerIn: parent; width: parent.width - 64; height: 360; radius: 18; color: "#263655"
            MouseArea { anchors.fill: parent }
            Column {
                anchors.fill: parent; anchors.margins: 24; spacing: 14
                Text { width: parent.width; wrapMode: Text.Wrap; color: "white"; font.pixelSize: 23; text: "Сколько будет 2 + 2?" }
                Text { width: parent.width; wrapMode: Text.Wrap; color: "white"; font.pixelSize: 17; text: "Введите ответ и нажмите «Проверить»." }
                Rectangle {
                    width: parent.width; height: 55; radius: 7; color: "white"
                    TextInput {
                        id: answer
                        anchors.fill: parent; anchors.margins: 12
                        color: "#18233a"; font.pixelSize: 22; inputMethodHints: Qt.ImhDigitsOnly
                        clip: true; selectByMouse: true
                    }
                }
                Text { width: parent.width; color: "#ffd080"; font.pixelSize: 16; text: first.validationMessage; visible: text.length > 0 }
                Rectangle {
                    width: parent.width; height: 55; radius: 8; color: "#526cc2"
                    Text { anchors.centerIn: parent; color: "white"; font.pixelSize: 18; text: "Проверить" }
                    MouseArea {
                        anchors.fill: parent
                        onClicked: {
                            Qt.inputMethod.hide()
                            if (first.checking) return
                            first.pendingAnswer = answer.text
                            first.checkAttempts = 0
                            first.checking = true
                            first.validationMessage = "Проверяем ответ в приложении…"
                            first.checkAnswer()
                        }
                    }
                }
            }
        }
    }
    Component {
        id: secondComponent
        SystemDialog {
            id: second
            property string statusText: "Выберите способ подключения тестового профиля."
            property int attempts: 0
            function checkVpnState() {
                vpnBridge.call("TestProfileState", [], function(reply) {
                    console.log("MATH_VPN: state", reply)
                    if (reply === "ok:ready" || reply === "ok:online") { second.statusText = "ConnMan подтвердил подключение: " + reply; return }
                    if (++second.attempts < 20) { stateTimer.restart(); return }
                    second.statusText = "Подключение не подтверждено: " + reply
                }, function(error) { second.statusText = "Ошибка проверки VPN: " + error })
            }
            Timer { id: stateTimer; interval: 500; onTriggered: second.checkVpnState() }
            contentHeight: screenHeight
            useDialogBackground: false
            color: "transparent"
            onDismissed: { console.log("MATH_VPN: second dialog dismissed"); first.done(first, false) }
            Item {
                width: parent.width; height: second.screenHeight
                Rectangle { anchors.fill: parent; color: "transparent" }
                MouseArea {
                    anchors.fill: parent
                    onClicked: { console.log("MATH_VPN: outside second dialog"); second.dismiss(); first.done(first, false) }
                }
                Rectangle {
                    anchors.centerIn: parent; width: parent.width - 64; height: 430; radius: 18; color: "#263655"
                    MouseArea { anchors.fill: parent }
                    Column {
                        anchors.fill: parent; anchors.margins: 24; spacing: 18
                        Text { width: parent.width; wrapMode: Text.Wrap; color: "white"; font.pixelSize: 23; text: "Ответ принят" }
                        Text { width: parent.width; wrapMode: Text.Wrap; color: "white"; font.pixelSize: 17; text: second.statusText }
                        Rectangle {
                            width: parent.width; height: 55; radius: 8; color: "#526cc2"
                            Text { anchors.centerIn: parent; color: "white"; font.pixelSize: 18; text: "Через приложение" }
                            MouseArea { anchors.fill: parent; onClicked: {
                                console.log("MATH_VPN: request application VPN")
                                second.statusText = "Приложение подключает VPN…"
                                answerService.call("ConnectVpn", [], function(ok) {
                                    console.log("MATH_VPN: application VPN result", ok)
                                    second.statusText = ok ? "Приложение подтвердило подключение VPN." : "Приложение не смогло подключить VPN; смотрите журнал."
                                }, function(error) { second.statusText = "Приложение недоступно: " + error })
                            } }
                        }
                        Rectangle {
                            width: parent.width; height: 55; radius: 8; color: "#526cc2"
                            Text { anchors.centerIn: parent; color: "white"; font.pixelSize: 18; text: "Через профиль ConnMan" }
                            MouseArea { anchors.fill: parent; onClicked: {
                                console.log("MATH_VPN: request direct profile VPN")
                                second.statusText = "Отправляем команду профилю…"
                                vpnConnection.call("Connect", [], function() {
                                    console.log("MATH_VPN: profile Connect reply")
                                    second.statusText = "Ожидаем состояние VPN…"
                                    second.attempts = 0; second.checkVpnState()
                                }, function(error) { console.log("MATH_VPN: profile Connect error", error); second.statusText = "Ошибка вызова профиля: " + error })
                            } }
                        }
                        Text {
                            width: parent.width; color: "white"; font.pixelSize: 17; horizontalAlignment: Text.AlignHCenter
                            text: "Закрыть"
                            MouseArea { anchors.fill: parent; onClicked: { second.dismiss(); first.done(first, false) } }
                        }
                    }
                }
            }
        }
    }
}
