# Aurora, упаковка и test VPN

Обычный Aurora RPM содержит пользовательское приложение. Задача `buildSystemVpnBundleAuroraArm64` собирает один development RPM с приложением, ConnMan plugin, test provider, D-Bus bridge, systemd unit и конфигурацией provider.

Bridge принимает только создание/поиск test-профиля, подключение, отключение и чтение состояния. Он не принимает произвольные маршруты, DNS или адреса.

Профиль `aurora-kmp-test-profile` использует `aurora_test_route`. При успехе ConnMan возвращает `ready`, появляется `vpn0` с адресом `10.203.0.2/30`, DNS и default route остаются без изменений.

RPM не подписан. Bypass validation допустим только в отдельном development overlay. Для физического устройства нужен доверенный system/extended signing profile и штатная Aurora validation policy.
