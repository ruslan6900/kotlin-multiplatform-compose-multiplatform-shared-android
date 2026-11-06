# UI, навигация и ресурсы

Приложение использует Compose Navigation. Главная страница открывает файловый менеджер, БД, ресурсы, библиотечные проверки и VPN-экран.

Ресурсы располагаются в `composeResources` и доступны через generated bindings `Res`. Для общих экранов используются строки, шрифты, PNG и SVG. XML VectorDrawable не следует считать переносимым общим форматом для Aurora Compose; для shared-ресурсов предпочтительны Compose Resources, PNG и SVG.

На Aurora 5.2 sandbox-запуск может оставить стартовый фон со spinner. Детали диагностики — в [отдельной заметке](../aurora-5.2-sandbox-ui-and-vpn-investigation.md).
