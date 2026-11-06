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
