# Архитектура и диагностика

```text
Compose UI / Navigation
        ↓
ViewModel / screen state
        ↓
Koin → repository → DAO → Room
        ↓
platform actual: storage, HTTP engine, Aurora services
```

VPN на Aurora использует отдельный ограниченный путь:

```text
Compose VPN screen → D-Bus bridge → ConnMan VPN → test provider → vpn0
```

UI-диагностика Aurora хранится в `/home/defaultuser/.local/share/com.example/aurorakmpdemo/ui-diagnostics.log`. Журнал библиотечных проверок хранится отдельно и заменяется следующим запуском. Секреты в журналы не записываются.
