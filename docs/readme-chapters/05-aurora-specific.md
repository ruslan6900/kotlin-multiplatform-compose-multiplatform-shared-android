# Aurora, упаковка и test VPN

Обычный Aurora RPM содержит пользовательское приложение. Задача `buildSystemVpnBundleAuroraArm64` собирает один development RPM с приложением, ConnMan plugin, test provider, D-Bus bridge, systemd unit и конфигурацией provider.

Bridge принимает только создание/поиск test-профиля, подключение, отключение и чтение состояния. Он не принимает произвольные маршруты, DNS или адреса.

## Как устроено подключение

```text
VPN-экран Compose
  → VpnController (Aurora actual)
  → com.example.aurorakmpdemo.VpnBridge, system D-Bus
  → net.connman.vpn.Manager / net.connman.vpn.Connection
  → aurora_test_route ConnMan plugin
  → com.example.aurorakmpdemo.test-route-vpn provider
  → vpn0
```

1. Пользователь нажимает «Создать и включить тестовый VPN».
2. `VpnController` вызывает `EnsureTestProfile(name)` у bridge. Bridge создаёт ConnMan profile с фиксированными `Type`, `Host` и `Domain`; если профиль уже есть, возвращает его известный путь.
3. Контроллер вызывает `ConnectTestProfile()` и опрашивает `TestProfileState()`. Ответ на `Connect` сам по себе не считается успешным: итогом является только `ready` или `online`.
4. ConnMan запускает native plugin. Plugin формирует TUN-настройки, split route и аргументы system D-Bus для provider.
5. Provider открывает созданный TUN, посылает ConnMan `notify("configuration", a{ss})` и ожидает подтверждение. После него ConnMan переводит соединение в `ready`.
6. При отключении bridge запрашивает `Disconnect`, plugin останавливает provider, а UI ждёт подтверждённое отключённое состояние.

Все этапы отображаются на VPN-экране и дублируются в UI-диагностике. Bridge не передаёт в ConnMan данные, введённые пользователем, кроме проверенного имени профиля.

## Роли файлов

| Файл | Роль |
| --- | --- |
| `PlatformVpnController.aurora.kt` | UI-состояние, D-Bus вызовы bridge и ожидание состояния |
| `bridge/main.cpp` | ограниченный privileged API для ConnMan |
| `plugin/split_tunnel_plugin.c` | драйвер ConnMan, TUN и route-конфигурация |
| `provider/test_route_provider.c` | handshake `notify(configuration)` и безопасный packet sink |
| `bridge/*.service`, `bridge/*.conf` | lifecycle systemd и D-Bus policy |
| `system-rpm/*` | упаковка всех частей в один development RPM |

Профиль `aurora-kmp-test-profile` использует `aurora_test_route`. При успехе ConnMan возвращает `ready`, появляется `vpn0` с адресом `10.203.0.2/30`, DNS и default route остаются без изменений.

## 1. Как собирается один RPM

Команда сборки:

```bash
./gradlew -PbuildVariant=aurora :composeApp:buildSystemVpnBundleAuroraArm64
```

Она запускает следующий конвейер.

1. `buildNativeTestVpnProviderAuroraArm64` кросс-собирает plugin, provider и bridge в Aurora SDK.
2. `buildAuroraArm64` собирает KMP/CMP-приложение для `aarch64`.
3. `buildSystemVpnBundleAuroraArm64` собирает временное дерево исходников и вызывает `rpmbuild` в контейнере Aurora SDK с custom spec `composeApp/system-rpm/aurora-kmp-demo-system.spec`.
4. Custom spec кладёт в **один** RPM пользовательское приложение и системные компоненты по их штатным путям.

В готовый RPM входят:

- приложение и его Compose/Kotlin-зависимости;
- ConnMan plugin: `/usr/lib/connman/plugins-vpn/aurora-test-route-proto-plugin.so`;
- test provider: `/usr/bin/com.example.aurorakmpdemo.test-route-vpn`;
- ограниченный D-Bus bridge, его systemd unit и D-Bus policy;
- конфигурация test provider для ConnMan.

Артефакт находится по пути:

```text
composeApp/system-rpm/.rpmbuild/aarch64/RPMS/aarch64/com.example.aurorakmpdemo-0.0.3-1.aarch64.rpm
```

Такое объединение возможно потому, что custom system spec владеет и пользовательскими, и системными путями. `extraFiles` обычного CMP RPM может включать файлы, но не заменяет system packaging policy и не даёт требуемых полномочий для установки ConnMan plugin на физическое устройство.

## 2. Как подписывается RPM

Development RPM намеренно не подписывается. Его допустимо ставить только на отдельный development overlay с bypass validation. Для физического устройства нужен доверенный Aurora **system/extended signing profile**: обычной GPG-подписи или self-signed сертификата недостаточно.

Подпись выполняется **после** сборки в отдельном защищённом окружении, где уже установлены Aurora signing tools и хранится выданная для целевого устройства цепочка сертификатов.

```bash
customer-sign \
  --binaries-key /защищённый-путь/binaries-key.pem \
  --packages-key /защищённый-путь/packages-key.pem \
  --packages-cert /защищённый-путь/packages-cert-extended.pem \
  --packages-client-cert /защищённый-путь/packages-cert-client.pem \
  /путь/к/com.example.aurorakmpdemo-0.0.3-1.aarch64.rpm

ompcert-cli dump /путь/к/com.example.aurorakmpdemo-0.0.3-1.aarch64.rpm
```

`customer-sign` добавляет package и binary signatures, а `ompcert-cli dump` позволяет проверить, что они действительно присутствуют. После успешной проверки RPM можно устанавливать стандартно:

```bash
pkcon install-local /путь/к/com.example.aurorakmpdemo-0.0.3-1.aarch64.rpm
```

Закрытые ключи, пароли и signing profile не должны передаваться в Gradle, храниться в Git, копироваться в каталог проекта или попадать в историю shell. В текущем pipeline `sign.set(false)` относится к обычному CMP RPM; custom system spec только упаковывает файлы и сознательно не обращается к ключам.
