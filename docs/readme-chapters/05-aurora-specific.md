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
| `packaging/system-rpm/*` | упаковка всех частей в один development RPM |

Профиль `aurora-kmp-test-profile` использует `aurora_test_route`. При успехе ConnMan возвращает `ready`, появляется `vpn0` с адресом `10.203.0.2/30`, DNS и default route остаются без изменений.

## Как собирается один RPM

Команда из корня проекта:

```bash
./gradlew -PbuildVariant=aurora :composeApp:buildSystemVpnBundleAuroraArm64
```

Для сборки нужны настроенный Aurora SDK с `qmake-aarch64` и Docker-образы, которые использует проект. Путь к SDK задаётся `-Paurora.sdkDir` или `AURORA_SDK_DIR`; сейчас Gradle по умолчанию использует `~/AuroraOS/sdk/5.2.1.200`. Конкретные образы (`aurora-build-tools:5.2.1.200` и `aurora-build-tools:5.2.0.180`) указаны в задачах и скриптах проекта.

Задача зависит от `buildDebugPackageAuroraArm64` и `buildNativeTestVpnProviderAuroraArm64`. Первая собирает Kotlin/Compose приложение и нативную библиотеку interop, обработчик фоновых задач и обычный RPM; скрипт `native/tasks/repack.sh` добавляет в desktop entry разделы `X-Task`. Вторая собирает ConnMan plugin, provider и bridge. Затем `packaging/system-rpm/build-aarch64.sh` проверяет наличие входных файлов, создаёт staging-каталог в `composeApp/build/system-rpm/aarch64`, копирует туда приложение, ресурсы, задачи, VPN-компоненты и QML-диалог с его службой, после чего запускает `rpmbuild` с `aurora-kmp-demo-system.spec`.

В **один основной RPM** входят приложение, иконки и ресурсы, вспомогательный процесс задач, библиотека interop, ConnMan plugin, тестовый provider, bridge и D-Bus policy, конфигурация VPN, QML-диалог, скрипт его запуска и два systemd unit. Отладочные RPM, создаваемые `rpmbuild`, — отдельные артефакты для отладки, а не обязательные дополнительные пакеты приложения. Результат текущей сборки:

```text
composeApp/build/system-rpm/aarch64/RPMS/aarch64/com.example.aurorakmpdemo-0.0.4-1.aarch64.rpm
```

Spec создаёт системные файлы, объявляет VPN-разрешение и для этого экспериментального системного RPM отключает Sailjail. В `%post` он пытается перечитать systemd/D-Bus, включить службы диалога и bridge и перезапустить bridge; ошибки этих команд не делают установку неуспешной, поэтому состояние служб нужно проверять отдельно. При удалении spec пытается отключить обе службы. На повторном старте приложение при необходимости просит bridge восстановить фиксированный manifest, удалённый системным sweeper после закрытия. Это обход для development-установки вне APM, а не подтверждённый способ промышленной поставки.

## Подпись и установка на тестовое устройство

Gradle и `build-aarch64.sh` **не подписывают** итоговый RPM: сразу после сборки он неподписан. Проверенные на физическом устройстве пакеты 0.0.3-15 и 0.0.4-1 были **подписан отдельной командой** с тестовым сертификатом `regular` и ключом из подготовленного набора Aurora SDK (`~/AuroraOS/package-signing/regular_cert.pem` и `regular_key.pem` в текущем окружении). Например, с доступными инструментами и этими файлами:

```bash
docker run --rm \
  -v "$PWD/composeApp/build/system-rpm/aarch64/RPMS/aarch64:/packages" \
  -v "$HOME/AuroraOS/package-signing:/sign:ro" \
  --entrypoint sh aurora-build-tools:5.2.1.200 -c \
  'rpmsign-external sign -k /sign/regular_key.pem -c /sign/regular_cert.pem /packages/com.example.aurorakmpdemo-0.0.4-1.aarch64.rpm'
```

Подпись модифицирует RPM после сборки; повторная сборка снова создаёт неподписанный файл и требует повторной подписи. Файлы сертификата и ключа находятся вне репозитория и не должны попадать в Git. Путь — пример текущего окружения, в другой установке SDK он может отличаться.

**Подпись и разрешённость содержимого — разные проверки.** Подписанный тестовым `regular` сертификатом единый RPM был отклонён штатным APM с `BadPackageContent`: системные пути, VPN, служба и внутренний интерфейс диалога выходят за рамки разрешённого состава. Проверка профилем `extended` также выявила ошибки. Нельзя заключить, что одна только замена сертификата на extended сделает пакет устанавливаемым; допустимый состав и системную интеграцию необходимо согласовать с платформой.

На выделенном тестовом устройстве версии 0.0.3-15 и 0.0.4-1 установлены от root **с разовым отключением RPM-плагина проверки содержимого** при сохранении других плагинов. Это не запуск без подписи и не штатная установка APM:

```sh
devel-su
rpm -U --define '__transaction_validation %{nil}' /путь/к/com.example.aurorakmpdemo-0.0.4-1.aarch64.rpm
rpm -q com.example.aurorakmpdemo
rpm -V --nodeps com.example.aurorakmpdemo
systemctl is-active com.example.aurorakmpdemo-vpn-bridge.service
systemctl is-enabled com.example.aurorakmpdemo-vpn-dialog.service
```

Если ранее установлен отдельный VPN development-пакет с теми же файлами, сначала нужно решить конфликт владения файлами при миграции; простое обновление может быть отклонено. На тестовом устройстве переход выполнялся отдельно с `--replacefiles` и удалением старой записи из RPM-базы без удаления файлов. Не применяйте этот обход как стандартный способ поставки.

На 0.0.4-1 проверены запуск и целостность файлов после установки; на 0.0.3-15 — повторное открытие приложения, ответ `4` через D-Bus и переход тестового VPN в `ready`. Тестовый provider не создаёт защищённый канал. Диалоги используют привилегированный внутренний интерфейс, поэтому наличие подписи само по себе не даёт обычному приложению право показывать их. Подробности: [системные диалоги](../aurora-system-dialogs.md), [уведомления и задачи](../aurora-system-events.md), [сеть и время](../aurora-network-and-time.md).
