#ifndef AURORA_INTEROP_PROBE_H
#define AURORA_INTEROP_PROBE_H
#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>
#include <dlfcn.h>

typedef struct {
    int8_t byte_value;
    int16_t short_value;
    int32_t int_value;
    int64_t long_value;
    uint8_t ubyte_value;
    uint16_t ushort_value;
    uint32_t uint_value;
    uint64_t ulong_value;
    float float_value;
    double double_value;
    bool bool_value;
    uint16_t char_value;
    size_t utf8_bytes;
    char text[128];
    int32_t array_sum;
    int32_t enum_value;
    bool pointer_preserved;
    bool null_seen;
} InteropProbeResult;

typedef enum { PROBE_MODE_ZERO = 0, PROBE_MODE_ONE = 1, PROBE_MODE_TWO = 2 } ProbeMode;
typedef int (*InteropProbeFn)(int8_t, int16_t, int32_t, int64_t, uint8_t, uint16_t,
    uint32_t, uint64_t, float, double, bool, uint16_t, const char *,
    const int32_t *, size_t, int32_t, const void *, const void *, InteropProbeResult *);

/* Вызов через dlopen позволяет показать ошибку загрузки вместо падения при старте приложения. */
static inline int interop_probe_call(int8_t b, int16_t s, int32_t i, int64_t l,
    uint8_t ub, uint16_t us, uint32_t ui, uint64_t ul, float f, double d,
    bool flag, uint16_t ch, const char *text, const int32_t *values, size_t count,
    int32_t mode, const void *identity, const void *nullable, InteropProbeResult *out) {
    void *handle = dlopen("/usr/share/com.example.aurorakmpdemo/lib/libaurora-interop-probe.so", RTLD_NOW | RTLD_LOCAL);
    if (!handle) return -1;
    InteropProbeFn probe = (InteropProbeFn)dlsym(handle, "aurora_interop_probe");
    if (!probe) { dlclose(handle); return -2; }
    int result = probe(b, s, i, l, ub, us, ui, ul, f, d, flag, ch, text,
        values, count, mode, identity, nullable, out);
    dlclose(handle);
    return result;
}
#endif
