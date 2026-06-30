package com.example.aurorakmpdemo.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

fun currentTimeTickerFlow(periodMs: Long = 1000L): Flow<String> = flow {
    while (true) {
        emit(formattedCurrentTime())
        delay(periodMs)
    }
}

fun formattedCurrentTime(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "${now.hour.twoDigits()}:${now.minute.twoDigits()}:${now.second.twoDigits()}"
}

private fun Int.twoDigits(): String = toString().padStart(length = 2, padChar = '0')
