package com.example.aurorakmpdemo.data

/** Общие Kotlin API, проверяемые Aurora-бинарником без платформенного кода. */
object KotlinLibraryChecks {
    fun run(): List<String> {
        val stages = CheckStage.entries.map { it.name.lowercase() }
        val copied = ProbeRecord("original", 1).copy(label = "copy")
        val grouped = listOf(1, 2, 3, 4).filter { it % 2 == 0 }.groupBy { it > 2 }
        val state = describe(CheckResult.Success(stages.size))

        return listOf(
            "enum.entries: ${stages.joinToString()}",
            "data class.copy: ${copied.label}#${copied.value}",
            "collections filter/groupBy: even=${grouped.values.flatten().joinToString()}",
            "sealed when: $state",
            "Result/runCatching: ${runCatching { 6 * 7 }.getOrThrow()}",
        )
    }

    private enum class CheckStage { Storage, Koin, Ui }

    private data class ProbeRecord(val label: String, val value: Int)

    private sealed interface CheckResult {
        data class Success(val count: Int) : CheckResult
        data class Failure(val reason: String) : CheckResult
    }

    private fun describe(result: CheckResult): String = when (result) {
        is CheckResult.Success -> "success(${result.count})"
        is CheckResult.Failure -> "failure(${result.reason})"
    }
}
