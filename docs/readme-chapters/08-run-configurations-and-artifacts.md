# Сборка, установка и запуск

Проверка окружения:

```bash
./gradlew -PbuildVariant=aurora doctorAuroraEnvironment
./gradlew -PbuildVariant=aurora verifyAuroraTarget
```

SSH-настройки задаются `-Paurora.host`, `-Paurora.port`, `-Paurora.user`, `-Paurora.sshKey` или соответствующими переменными окружения.

Визуальный debug-запуск:

```bash
./gradlew -PbuildVariant=aurora :composeApp:runDebugOnEmulatorNoSandbox
```

`--nosandbox` предназначен только для диагностики emulator: он не подтверждает Sailjail permissions и не годится для production.

Test VPN RPM собирается командой:

```bash
./gradlew -PbuildVariant=aurora :composeApp:buildSystemVpnBundleAuroraArm64
```

После development-установки активируйте bridge:

```bash
systemctl enable --now com.example.aurorakmpdemo-vpn-bridge
systemctl status com.example.aurorakmpdemo-vpn-bridge
```
