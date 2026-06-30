import java.util.Properties

plugins {
    id("com.diffplug.spotless") version "8.4.0"
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint("1.6.0")
    }
    format("misc") {
        target("**/*.gradle.kts", "**/*.md", "**/.gitignore", "**/*.xml", "**/*.properties")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

val localProperties = rootProject.file("local.properties")
    .takeIf { it.exists() }
    ?.inputStream()
    ?.use { Properties().apply { load(it) } }

val sdkDir = localProperties?.getProperty("sdk.dir")
val adbPath = sdkDir?.let { "$it/platform-tools/adb" }
val emulatorPath = sdkDir?.let { "$it/emulator/emulator" }
val defaultAvd = "Pixel_8_Pro"

tasks.register<Exec>("runDesktopMain") {
    group = "run"
    description = "Run the desktop target with the main build variant."
    commandLine("./gradlew", "-PbuildVariant=main", ":composeApp:run")
}

tasks.register<Exec>("assembleAndroidMain") {
    group = "build"
    description = "Assemble the Android debug build with the main build variant."
    commandLine("./gradlew", "-PbuildVariant=main", ":composeApp:assembleDebug")
}

tasks.register<Exec>("installAndroidMain") {
    group = "run"
    description = "Install the Android debug build on the currently connected device or emulator."
    commandLine("./gradlew", "-PbuildVariant=main", ":composeApp:installDebug")
}

tasks.register<Exec>("startAndroidEmulator") {
    group = "run"
    description = "Start a macOS Apple Silicon compatible Android emulator."
    doFirst {
        require(emulatorPath != null) { "Android emulator not found. Check local.properties sdk.dir." }
    }
    commandLine(
        "sh",
        "-c",
        "${emulatorPath ?: "echo missing-emulator"} -avd $defaultAvd >/tmp/${defaultAvd}.log 2>&1 &"
    )
}

tasks.register<Exec>("launchAndroidApp") {
    group = "run"
    description = "Launch the installed Android app on the active device."
    doFirst {
        require(adbPath != null) { "adb not found. Check local.properties sdk.dir." }
    }
    commandLine(
        "sh",
        "-c",
        "${adbPath ?: "echo missing-adb"} shell am start -n com.example.aurorakmpdemo/.MainActivity"
    )
}

tasks.register<Exec>("runAndroidMain") {
    group = "run"
    description = "Install and launch the Android app on the active device."
    commandLine(
        "sh",
        "-c",
        "./gradlew -PbuildVariant=main :composeApp:installDebug && ${adbPath ?: "echo missing-adb"} shell am start -n com.example.aurorakmpdemo/.MainActivity"
    )
}

tasks.register<Exec>("buildAuroraDebug") {
    group = "aurora"
    description = "Build the Aurora debug RPM pipeline."
    commandLine("./gradlew", "-PbuildVariant=aurora", ":composeApp:buildDebugPipeline")
}

tasks.register<Exec>("runAuroraOnEmulator") {
    group = "aurora"
    description = "Build and run the Aurora app on an Aurora emulator."
    commandLine(
        "sh",
        "-c",
        "./gradlew -PbuildVariant=aurora :composeApp:buildDebugPipeline && ./gradlew -PbuildVariant=aurora :composeApp:runDebugOnEmulator"
    )
}
