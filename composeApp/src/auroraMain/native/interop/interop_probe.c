#include "interop_probe.h"
#include <stdio.h>
#include <string.h>

/* Фиксированные по размеру типы C соответствуют тестируемым значениям Kotlin. */
int aurora_interop_probe(int8_t b, int16_t s, int32_t i, int64_t l,
    uint8_t ub, uint16_t us, uint32_t ui, uint64_t ul, float f, double d,
    bool flag, uint16_t ch, const char *text, const int32_t *values, size_t count,
    int32_t mode, const void *identity, const void *nullable, InteropProbeResult *out) {
    if (!out || !text || (!values && count) || strlen(text) >= sizeof(out->text)) return -3;
    if (mode != PROBE_MODE_ONE) return -4;
    out->byte_value = b + 1;
    out->short_value = s + 1;
    out->int_value = i + 1;
    out->long_value = l + 1;
    out->ubyte_value = ub + 1;
    out->ushort_value = us + 1;
    out->uint_value = ui + 1;
    out->ulong_value = ul + 1;
    out->float_value = f * 2.0f;
    out->double_value = d * 2.0;
    out->bool_value = !flag;
    out->char_value = ch + 1;
    out->utf8_bytes = strlen(text);
    snprintf(out->text, sizeof(out->text), "%s", text);
    out->array_sum = 0;
    for (size_t index = 0; index < count; ++index) out->array_sum += values[index];
    out->enum_value = mode + 1;
    out->pointer_preserved = identity == (const void *)values;
    out->null_seen = nullable == NULL;
    return 0;
}
