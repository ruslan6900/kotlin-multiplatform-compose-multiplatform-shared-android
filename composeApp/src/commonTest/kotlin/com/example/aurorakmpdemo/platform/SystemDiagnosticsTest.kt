package com.example.aurorakmpdemo.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SystemDiagnosticsTest {
    @Test fun `network connection is not confused with validated internet`() {
        assertEquals(NetworkAvailability.Online, classifyConnmanState("online"))
        assertEquals(NetworkAvailability.ConnectedUnverified, classifyConnmanState("ready"))
        assertEquals(NetworkAvailability.Offline, classifyConnmanState("idle"))
        assertEquals(NetworkAvailability.Unknown, classifyConnmanState("unexpected"))
    }

    @Test fun `clock changes differ from elapsed uptime including suspension`() {
        assertNull(clockAdjustmentMillis(100_000, 101_000, 8_000, 9_000))
        // One minute passed during sleep on both clocks: not a wall-clock change.
        assertNull(clockAdjustmentMillis(100_000, 160_000, 8_000, 68_000))
        assertEquals(3_600_000, clockAdjustmentMillis(100_000, 3_701_000, 8_000, 9_000))
        assertEquals(-60_000, clockAdjustmentMillis(100_000, 41_000, 8_000, 9_000))
        assertNull(clockAdjustmentMillis(100_000, 101_499, 8_000, 9_000))
        assertNull(clockAdjustmentMillis(100_000, 101_000, 8_000, 7_000))
    }
}
