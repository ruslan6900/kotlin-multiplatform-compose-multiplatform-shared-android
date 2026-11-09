package com.example.aurorakmpdemo.platform

import com.example.aurorakmpdemo.interop.InteropProbeResult
import com.example.aurorakmpdemo.interop.interop_probe_call
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual fun runNativeInteropProbe(): List<NativeInteropCheck>? = memScoped {
    val result = alloc<InteropProbeResult>()
    val array = allocArray<IntVar>(3)
    array[0] = 10; array[1] = -3; array[2] = 5
    val original = "Привет, Aurora!"
    val code = interop_probe_call(
        (-12).toByte(), (-32000).toShort(), -123456, -5000000000L,
        250u.toUByte(), 65000u.toUShort(), 4000000000u, 10000000000uL,
        1.25f, -2.5, true, 'Ж'.code.toUShort(), original,
        array, 3u, 1, array, null, result.ptr,
    )
    check(code == 0) { "Ошибка загрузки или вызова libaurora-interop-probe.so: код=$code (-1: dlopen, -2: dlsym)" }
    val checks = listOf(
        Triple("int8_t / Kotlin Byte", result.byte_value == (-11).toByte(), "-12 + 1 = ${result.byte_value}"),
        Triple("int16_t / Short", result.short_value == (-31999).toShort(), "-32000 + 1 = ${result.short_value}"),
        Triple("int32_t / Int", result.int_value == -123455, "-123456 + 1 = ${result.int_value}"),
        Triple("int64_t / Long", result.long_value == -4999999999L, "-5000000000 + 1 = ${result.long_value}"),
        Triple("uint8_t / UByte", result.ubyte_value == 251u.toUByte(), "250 + 1 = ${result.ubyte_value}"),
        Triple("uint16_t / UShort", result.ushort_value == 65001u.toUShort(), "65000 + 1 = ${result.ushort_value}"),
        Triple("uint32_t / UInt", result.uint_value == 4000000001u, "4000000000 + 1 = ${result.uint_value}"),
        Triple("uint64_t / ULong", result.ulong_value == 10000000001uL, "10000000000 + 1 = ${result.ulong_value}"),
        Triple("float / Float", result.float_value == 2.5f, "1.25 × 2 = ${result.float_value}"),
        Triple("double / Double", result.double_value == -5.0, "-2.5 × 2 = ${result.double_value}"),
        Triple("_Bool / Boolean", !result.bool_value, "!true = ${result.bool_value}"),
        Triple("uint16_t / Char UTF-16", result.char_value == ('Ж'.code + 1).toUShort(), "Ж + 1 = ${result.char_value.toInt().toChar()}"),
        Triple("char* / UTF-8 String", result.text.toKString() == original && result.utf8_bytes.toLong() == original.encodeToByteArray().size.toLong(), "строка=${result.text.toKString()}, байт=${result.utf8_bytes}"),
        Triple("int32_t* / массив + size_t", result.array_sum == 12, "10 + (-3) + 5 = ${result.array_sum}"),
        Triple("enum / int32_t", result.enum_value == 2, "PROBE_MODE_ONE → ${result.enum_value}"),
        Triple("void* / указатели", result.pointer_preserved && result.null_seen, "адрес сохранён=${result.pointer_preserved}, null=${result.null_seen}"),
    )
    checks.map { (name, success, detail) -> NativeInteropCheck(name, success, detail) }
}
