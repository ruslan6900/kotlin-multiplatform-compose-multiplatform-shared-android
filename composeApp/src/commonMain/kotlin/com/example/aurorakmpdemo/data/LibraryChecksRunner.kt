package com.example.aurorakmpdemo.data

import androidx.lifecycle.ViewModel
import androidx.room.RoomRawQuery
import co.touchlab.stately.collections.ConcurrentMutableList
import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.collections.ConcurrentMutableSet
import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.AtomicLong
import co.touchlab.stately.concurrency.AtomicReference
import co.touchlab.stately.concurrency.value
import com.example.aurorakmpdemo.db.LibraryProbeEntity
import com.example.aurorakmpdemo.db.LibraryProbeEventEntity
import com.example.aurorakmpdemo.db.PostsDao
import com.example.aurorakmpdemo.db.PostsRoomDatabase
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.Channel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.io.decodeFromSource
import kotlinx.serialization.json.io.encodeToSink
import kotlinx.io.Buffer
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlin.time.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

data class LibraryCheckEvent(
    val area: String,
    val success: Boolean,
    val detail: String,
)

data class LibraryCheckRequest(val label: String, val prefix: String)

class InjectedLibraryPrefix : KoinComponent {
    val value: String by inject(qualifier = named("library-check-prefix"))
}

class LibraryChecksViewModel(val runner: LibraryChecksRunner) : ViewModel() {
    private val _lastUiEvent = MutableStateFlow("Ожидание запуска")
    val lastUiEvent = _lastUiEvent

    fun recordUiEvent(value: String) {
        _lastUiEvent.value = value
    }
}

@Serializable(with = ProbeLabelSerializer::class)
data class ProbeLabel(val value: String)

object ProbeLabelSerializer : KSerializer<ProbeLabel> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ProbeLabel", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ProbeLabel) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): ProbeLabel = ProbeLabel(decoder.decodeString())
}

@Serializable
data class SerializationProbe(
    @SerialName("probe_label") val label: ProbeLabel,
    val values: List<Int>,
    @Transient val runtimeOnly: String = "not-serialized",
)

/** Runtime-проверка переносимых API с выводом каждого этапа в UI. */
class LibraryChecksRunner(
    private val database: PostsRoomDatabase,
    private val dao: PostsDao,
    private val client: HttpClient,
    private val koinPrefix: String,
) {
    @OptIn(FlowPreview::class, ExperimentalSerializationApi::class)
    suspend fun run(report: (LibraryCheckEvent) -> Unit) {
        suspend fun checked(area: String, block: suspend () -> String) {
            runCatching { block() }
                .onSuccess { report(LibraryCheckEvent(area, true, it)) }
                .onFailure { report(LibraryCheckEvent(area, false, "${it::class.simpleName}: ${it.message}")) }
        }

        checked("Kotlin stdlib") {
            val enumValues = CheckStage.entries.joinToString()
            val copied = CheckValue("source", 1).copy(name = "copy")
            val grouped = listOf(1, 2, 3, 4).filter { it % 2 == 0 }.groupBy { it > 2 }
            val sealed = describe(CheckResult.Success(grouped.size))
            "entries=$enumValues, copy=$copied, collections=${grouped.values.flatten()}, sealed=$sealed, Result=${runCatching { 6 * 7 }.getOrThrow()}"
        }

        checked("Koin") {
            val singletonMatches = database === org.koin.mp.KoinPlatform.getKoin().get<PostsRoomDatabase>()
            val request = org.koin.mp.KoinPlatform.getKoin().get<LibraryCheckRequest> {
                org.koin.core.parameter.parametersOf("runtime")
            }
            val injected = InjectedLibraryPrefix().value
            val viewModel = org.koin.mp.KoinPlatform.getKoin().get<LibraryChecksViewModel>()
            "singleton=$singletonMatches, DAO=${dao::class.simpleName}, factory=${request.prefix}:${request.label}, inject=$injected, viewModel=${viewModel::class.simpleName}"
        }

        checked("Coroutines / Flow") {
            val state = MutableStateFlow(0).also { it.update { current -> current + 1 } }
            val shared = MutableSharedFlow<String>(replay = 1).also { it.emit("shared") }
            val channel = Channel<Int>(capacity = 1).also { it.send(7) }
            val callback = callbackFlow {
                trySend("callback")
                close()
                awaitClose { }
            }.first()
            val combined = flowOf("left").combine(flowOf("right")) { left, right -> "$left/$right" }.first()
            val debounced = flowOf("debounce").debounce(1).first()
            val parallel = coroutineScope { listOf(async { 2 }, async { 3 }).awaitAll().sum() }
            val timeout = withTimeout(100) { delay(1); "ok" }
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val stateIn = flowOf(3, 4).stateIn(scope, SharingStarted.Eagerly, 0)
                val shareIn = flowOf("shared-in").shareIn(scope, SharingStarted.Eagerly, replay = 1)
                val collected = mutableListOf<Int>()
                flowOf(1, null, 2)
                    .filterNotNull()
                    .map { it * 10 }
                    .onEach { collected += it }
                    .collectLatest { }
                val deferred = CompletableDeferred<String>()
                withContext(Dispatchers.Default) { deferred.complete("context-ok") }
                delay(1)
                "StateFlow=${state.value}/${stateIn.value}, SharedFlow=${shared.first()}/${shareIn.first()}, Channel=${channel.receive()}, callback=$callback, combine=$combined, debounce=$debounced, scope=$parallel, flowOps=$collected, deferred=${deferred.await()}, timeout=$timeout"
            } finally {
                scope.cancel()
            }
        }

        checked("Serialization") {
            val source = SerializationProbe(ProbeLabel("portable"), listOf(1, 2, 3), runtimeOnly = "runtime")
            val json = Json { encodeDefaults = true; ignoreUnknownKeys = true; explicitNulls = false }
            val encoded = json.encodeToString(SerializationProbe.serializer(), source)
            val decoded = json.decodeFromString(SerializationProbe.serializer(), encoded)
            val ioJsonBuffer = Buffer()
            json.encodeToSink(SerializationProbe.serializer(), source, ioJsonBuffer)
            val decodedFromIo = json.decodeFromSource(SerializationProbe.serializer(), ioJsonBuffer)
            val tree: JsonObject = buildJsonObject { put("format", "json-tree"); put("count", decoded.values.size) }
            "custom serializer + SerialName/Transient + JSON/IO round-trip: ${decoded.label.value}/${decodedFromIo.label.value}, values=${decoded.values}, runtime=${decoded.runtimeOnly}, tree=$tree, bytes=${encoded.length}"
        }

        checked("Date/time") {
            val now = Clock.System.now()
            val future = now.plus(1, DateTimeUnit.HOUR, TimeZone.UTC)
            val local = future.toLocalDateTime(TimeZone.UTC)
            "Instant arithmetic + UTC conversion: ${local.hour}:${local.minute}"
        }

        checked("Room CRUD / Flow") {
            val now = Clock.System.now().toEpochMilliseconds()
            val inserted = LibraryProbeEntity(9001, "library-check", "inserted", listOf("room", "converter"), now)
            dao.insertLibraryProbeWithEvent(
                probe = inserted,
                event = LibraryProbeEventEntity(90011, inserted.id, "transactional insert", now),
            )
            val observedInserted = dao.observeLibraryProbe(inserted.id).first()?.stage
            val updated = inserted.copy(stage = "updated", tags = inserted.tags + "flow")
            val updateCount = dao.updateLibraryProbe(updated)
            val relation = dao.selectLibraryProbeWithEvents(inserted.id)
            val raw = dao.selectLibraryProbesRaw(RoomRawQuery("SELECT * FROM library_probe WHERE id = 9001"))
            val deleteCount = dao.deleteLibraryProbe(updated)
            "transaction=$observedInserted, update=$updateCount, relationEvents=${relation?.events?.size}, raw=${raw.size}, converterTags=${relation?.probe?.tags}, delete=$deleteCount, db=${database::class.simpleName}"
        }

        checked("Ktor HTTP") {
            val statement = client.prepareGet("https://jsonplaceholder.typicode.com/posts/1") {
                parameter("libraryCheck", "aurora")
                header("X-Aurora-Kmp-Demo", "library-check")
            }.execute()
            @Suppress("DEPRECATION")
            val bytes = statement.bodyAsChannel().readRemaining().readBytes()
            check(statement.status.isSuccess()) { "HTTP ${statement.status.value}" }
            val notFound: HttpResponse = client.prepareGet("https://jsonplaceholder.typicode.com/missing-library-check").execute()
            check(notFound.status.value == 404) { "Expected 404, got ${notFound.status.value}" }
            "prepareGet/query/header/status/body stream + error status: HTTP ${statement.status.value}, ${bytes.size} bytes; 404=${notFound.status.value}"
        }

        checked("In-memory IO / concurrency") {
            val kotlinxBuffer = Buffer().apply { writeString("kotlinx-io") }
            val list = ConcurrentMutableList<String>().apply { add("one"); add("two") }
            val map = ConcurrentMutableMap<String, Int>().apply { put("checks", 2) }
            val set = ConcurrentMutableSet<String>().apply { add("aurora") }
            val enabled = AtomicBoolean(false)
            val counter = AtomicLong(41)
            val reference = AtomicReference("before")
            val changed = enabled.compareAndSet(false, true)
            val total = counter.incrementAndGet()
            reference.compareAndSet("before", "after")
            "kotlinx-io=${kotlinxBuffer.readString()}, collections=${list.size}/${map["checks"]}/${set.contains("aurora")}, atomic=$changed/$total/${reference.value}"
        }
    }

    private enum class CheckStage { Storage, Koin, Ui }
    private data class CheckValue(val name: String, val value: Int)
    private sealed interface CheckResult { data class Success(val value: Int) : CheckResult; data class Failure(val reason: String) : CheckResult }
    private fun describe(value: CheckResult): String = when (value) { is CheckResult.Success -> "success(${value.value})"; is CheckResult.Failure -> "failure(${value.reason})" }
}
