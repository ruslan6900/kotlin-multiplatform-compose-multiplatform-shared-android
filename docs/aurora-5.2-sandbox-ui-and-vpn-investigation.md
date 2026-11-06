# Aurora 5.2: sandbox UI и test VPN

Дата фиксации: 2026-11-06. Среда: AuroraOS 5.2.0.180 emulator, `aarch64`, приложение `com.example.aurorakmpdemo`.

## Два независимых поведения

1. В `startDebug --nosandbox` Compose UI отображается. Этот режим не имеет Sailjail context и не доказывает доступ к системному ConnMan.
2. При sandbox-запуске на emulator 5.2 иногда остаётся стартовый фон со spinner. Собственный журнал доходит до Koin и корневой Compose-композиции; в system journal наблюдался crash графического/clipboard runtime. Это похоже на проблему emulator, Wayland и Compose runtime, а не на навигацию или Room.

## Подтверждённый VPN lifecycle

Объединённый development RPM устанавливает test provider и ограниченный system D-Bus bridge. Bridge создаёт или находит `aurora-kmp-test-profile`, запрашивает подключение, provider передаёт конфигурацию ConnMan по system D-Bus, ConnMan возвращает `State=ready`, затем появляется `vpn0`.

Provider обслуживает `198.51.100.0/24`, не меняет default route и DNS и отбрасывает трафик. Это тест ConnMan-интеграции, не сетевой канал.

## Диагностика

```text
/home/defaultuser/.local/share/com.example/aurorakmpdemo/ui-diagnostics.log
```

```bash
systemctl status com.example.aurorakmpdemo-vpn-bridge
journalctl -u com.example.aurorakmpdemo-vpn-bridge -u connman-vpn
```

Команде Aurora следует подтвердить поддерживаемую конфигурацию Compose/Sailjail/Wayland для emulator 5.2 либо предоставить исправление sandbox-rendering. До этого `--nosandbox` допустим только для визуальной development-проверки; VPN-операции должны оставаться в минимальном подписанном bridge.
