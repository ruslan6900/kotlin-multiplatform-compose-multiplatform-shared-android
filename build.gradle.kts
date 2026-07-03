import java.util.Properties

plugins {
    id("com.diffplug.spotless") version "8.4.0"
}

version = "0.0.2"

val buildVariant = providers.gradleProperty("buildVariant").orElse("main")

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

fun resolveAndroidSdkDir(): String? = sequenceOf(
    localProperties?.getProperty("sdk.dir"),
    System.getenv("ANDROID_SDK_ROOT"),
    System.getenv("ANDROID_HOME"),
).firstOrNull { !it.isNullOrBlank() }

fun resolveToolPath(explicitPath: String?, sdkRelativePath: String, fallbackBinaryName: String): String {
    return explicitPath
        ?: resolveAndroidSdkDir()?.let { "$it/$sdkRelativePath" }
        ?: fallbackBinaryName
}

val sdkDir = resolveAndroidSdkDir()
val adbPath = resolveToolPath(
    explicitPath = localProperties?.getProperty("adb.path"),
    sdkRelativePath = "platform-tools/adb",
    fallbackBinaryName = "adb",
)
val emulatorPath = resolveToolPath(
    explicitPath = localProperties?.getProperty("emulator.path"),
    sdkRelativePath = "emulator/emulator",
    fallbackBinaryName = "emulator",
)
val defaultAvd = localProperties?.getProperty("android.avd")
    ?: System.getenv("ANDROID_AVD")
    ?: "Pixel_8_Pro"

fun commandExists(command: String): Boolean {
    val result = providers.exec {
        commandLine("sh", "-c", "command -v $command >/dev/null 2>&1")
        isIgnoreExitValue = true
    }
    return result.result.get().exitValue == 0
}

tasks.register("doctorMainEnvironment") {
    group = "verification"
    description = "Print detected toolchain paths for the main KMP targets."
    notCompatibleWithConfigurationCache("Diagnostic environment checks use shell lookups and host-specific state.")

    doLast {
        println("== Main Environment Doctor ==")
        println("buildVariant=${buildVariant.get()}")
        println("sdk.dir=${sdkDir ?: "<not set>"}")
        println("ANDROID_SDK_ROOT=${System.getenv("ANDROID_SDK_ROOT") ?: "<not set>"}")
        println("ANDROID_HOME=${System.getenv("ANDROID_HOME") ?: "<not set>"}")
        println("adb.path=$adbPath")
        println("emulator.path=$emulatorPath")
        println("android.avd=$defaultAvd")
        println("JAVA_HOME=${System.getenv("JAVA_HOME") ?: "<not set>"}")
        println("MAIN_JAVA_HOME=${System.getenv("MAIN_JAVA_HOME") ?: "<not set>"}")
        println("xcodebuild.available=${commandExists("xcodebuild")}")
        println("xcrun.available=${commandExists("xcrun")}")
        println("adb.available=${commandExists("adb")}")
        println("emulator.available=${commandExists("emulator")}")
    }
}

tasks.register("doctorAuroraEnvironment") {
    group = "verification"
    description = "Print detected Aurora SDK / emulator settings."
    notCompatibleWithConfigurationCache("Diagnostic environment checks use shell lookups and host-specific state.")

    doLast {
        val auroraSshKey = providers.gradleProperty("aurora.sshKey")
            .orElse(providers.environmentVariable("AURORA_SSH_KEY"))
            .orElse("${System.getProperty("user.home")}/AuroraOS/vmshare/ssh/private_keys/sdk")
            .get()
        val auroraHost = providers.gradleProperty("aurora.host")
            .orElse(providers.environmentVariable("AURORA_HOST"))
            .orElse("127.0.0.1")
            .get()
        val auroraUser = providers.gradleProperty("aurora.user")
            .orElse(providers.environmentVariable("AURORA_USER"))
            .orElse("defaultuser")
            .get()
        val auroraPort = providers.gradleProperty("aurora.port")
            .orElse(providers.environmentVariable("AURORA_PORT"))
            .orElse("2223")
            .get()
        val auroraRepoDir = rootProject.file("work/aurora-maven-partial")

        println("== Aurora Environment Doctor ==")
        println("buildVariant=${buildVariant.get()}")
        println("aurora.host=$auroraHost")
        println("aurora.port=$auroraPort")
        println("aurora.user=$auroraUser")
        println("aurora.sshKey=$auroraSshKey")
        println("aurora.sshKey.exists=${file(auroraSshKey).exists()}")
        println("aurora.maven.partial=${auroraRepoDir.absolutePath}")
        println("aurora.maven.partial.exists=${auroraRepoDir.exists()}")
        println("JAVA_HOME=${System.getenv("JAVA_HOME") ?: "<not set>"}")
        println("AURORA_JAVA_HOME=${System.getenv("AURORA_JAVA_HOME") ?: "<not set>"}")
        println("ssh.available=${commandExists("ssh")}")
    }
}

tasks.register("verifyMainTargets") {
    group = "verification"
    description = "Compile or assemble the supported main targets: Android, Desktop and iOS simulator."
    dependsOn(
        "doctorMainEnvironment",
        ":composeApp:assembleDebug",
        ":composeApp:compileKotlinDesktop",
        ":composeApp:linkDebugFrameworkIosSimulatorArm64",
    )
}

tasks.register("verifyAuroraTarget") {
    group = "verification"
    description = "Check the Aurora environment and build the Aurora debug pipeline."
    dependsOn(
        "doctorAuroraEnvironment",
        ":composeApp:buildDebugPipeline",
    )
}

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
    commandLine(
        "sh",
        "-c",
        "$emulatorPath -avd $defaultAvd >/tmp/${defaultAvd}.log 2>&1 &"
    )
}

tasks.register<Exec>("launchAndroidApp") {
    group = "run"
    description = "Launch the installed Android app on the active device."
    commandLine(
        "sh",
        "-c",
        "$adbPath shell am start -n com.example.aurorakmpdemo/.MainActivity"
    )
}

tasks.register<Exec>("runAndroidMain") {
    group = "run"
    description = "Install and launch the Android app on the active device."
    commandLine(
        "sh",
        "-c",
        "./gradlew -PbuildVariant=main :composeApp:installDebug && $adbPath shell am start -n com.example.aurorakmpdemo/.MainActivity"
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
