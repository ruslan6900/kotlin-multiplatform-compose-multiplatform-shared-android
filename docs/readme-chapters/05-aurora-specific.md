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

RPM не подписан. Bypass validation допустим только в отдельном development overlay. Для физического устройства нужен доверенный system/extended signing profile и штатная Aurora validation policy.

## Как собирается один RPM

`buildNativeTestVpnProviderAuroraArm64` кросс-собирает plugin, provider и bridge в Aurora SDK. Затем `buildSystemVpnBundleAuroraArm64` сначала собирает обычное KMP-приложение, кладёт его и нативные компоненты во временный source tree и запускает `rpmbuild` в контейнере Aurora SDK.

В итоговый RPM входят приложение, `/usr/lib/connman/plugins-vpn/aurora-test-route-proto-plugin.so`, provider, bridge, systemd unit, D-Bus policy и ConnMan-конфигурация. Автоматической подписи в этом pipeline нет: `sign.set(false)` относится к обычному RPM, а custom spec только упаковывает файлы. Подпись должна выполняться отдельным доверенным процессом после сборки.
