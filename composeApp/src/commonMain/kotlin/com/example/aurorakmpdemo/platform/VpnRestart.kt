package com.example.aurorakmpdemo.platform

const val DEFAULT_TEST_VPN_PROFILE = "aurora-kmp-test-profile"

data class VpnRestartSettings(
    val enabled: Boolean = false,
    val profileId: String = DEFAULT_TEST_VPN_PROFILE,
    val handledBootId: String = "",
)

fun isTestVpnProfileValid(profile: String): Boolean =
    profile.length in 1..64 && profile.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it in "._ -" }

fun shouldPromptVpnAfterReboot(settings: VpnRestartSettings, bootId: String): Boolean =
    settings.enabled && bootId.isNotBlank() && settings.handledBootId.isNotBlank() && settings.handledBootId != bootId

/** Persistent settings and current boot identity. Failure must not look like a successful configuration. */
expect fun readVpnRestartSettings(): Result<VpnRestartSettings>
expect fun saveVpnRestartSettings(settings: VpnRestartSettings): Result<Unit>
expect fun currentVpnBootId(): Result<String>
