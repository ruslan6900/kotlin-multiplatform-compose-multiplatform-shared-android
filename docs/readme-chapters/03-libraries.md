# Библиотеки и проверки

Экран «Библиотеки» выполняет операции последовательно и сохраняет подробный журнал; новый запуск заменяет прошлый результат.

| Компонент | Проверяемые операции |
| --- | --- |
| Kotlin stdlib | enum, data class, collections, sealed class, `Result` |
| Coroutines | `Flow`, `SharedFlow`, `Channel`, cancellation и timeout |
| Serialization | JSON, custom serializer и round trip |
| DateTime | `Instant`, timezone и преобразование времени |
| Koin | singleton, DAO, repository и ViewModel |
| Room KMP | migration, CRUD, relation, converter, Flow |
| Ktor | HTTP-запрос и обработка ответа |
| Compose | navigation, state и ресурсы |

Room реализован цепочкой entity → DAO → database → repository. Android-only зависимости, test-библиотеки, FileKit и локальные модули в этой проверке не участвуют.

На этом же экране доступны [проверки сети и системного времени](../aurora-network-and-time.md): разовая проверка и наблюдение за типом и доступностью сети, а также отслеживание изменения часов и часового пояса. На отдельном экране показаны [локальные уведомления и фоновые задачи](../aurora-system-events.md).
