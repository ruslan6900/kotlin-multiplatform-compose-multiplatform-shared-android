plugins {
    kotlin("multiplatform") version "2.3.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
    id("com.google.devtools.ksp") version "2.3.9"
    id("androidx.room") version "2.8.4"
    id("ru.auroraos.kmp.aurora-build") version "0.0.1"
    id("ru.auroraos.kmp.aurora-devices") version "0.0.1"
    id("org.jetbrains.compose") version "0.0.4-aurora"
}

auroraBuild {
    targets {
        arm64.set("auroraArm64")
        x64.set("auroraX64")
    }
    rpm {
        id.set("com.example.aurorakmpdemo")
        name.set("Aurora KMP Demo")
        description.set("Aurora Kotlin Multiplatform compatibility check.")
        icons.set(projectDir.toPath().resolve("icons"))
        libs3rdParty.set(listOf("maliit-glib"))
        permissions.set(listOf("Internet", "UserDirs"))
    }
}

auroraDevices {
    packages {
        create("release") {
            targets.set(listOf("aarch64", "x86_64"))
            directory.set(projectDir.toPath().resolve("build/rpm/release/{target}/RPMS/{target}"))
        }
        create("debug") {
            targets.set(listOf("aarch64", "x86_64"))
            directory.set(projectDir.toPath().resolve("build/rpm/debug/{target}/RPMS/{target}"))
        }
    }
}

val auroraHost = "127.0.0.1"
val auroraUser = "defaultuser"
val auroraPort = "2223"
val auroraSshKey = "${System.getProperty("user.home")}/AuroraOS/vmshare/ssh/private_keys/sdk"
val auroraAppId = "com.example.aurorakmpdemo"

tasks.register<Exec>("killAuroraDemoOnEmulator") {
    group = "Aurora Devices"
    description = "Stops any running Aurora KMP Demo processes inside the emulator before the next launch."
    notCompatibleWithConfigurationCache("Uses local SSH exec calls to kill emulator-side app processes.")
    isIgnoreExitValue = true
    commandLine(
        "bash",
        "-lc",
        """
        ssh \
          -o StrictHostKeyChecking=no \
          -o UserKnownHostsFile=/dev/null \
          -o LogLevel=ERROR \
          -p $auroraPort \
          -i $auroraSshKey \
          $auroraUser@$auroraHost \
          '
          ps aux | grep -E "(/usr/bin/$auroraAppId|private-bin=$auroraAppId)" | grep -v grep | awk '"'"'{print ${'$'}2}'"'"' | xargs -r kill -9 2>/dev/null || true
          sleep 1
          ps aux | grep -i "$auroraAppId" | grep -v grep || true
          exit 0
          '
        """.trimIndent(),
    )
}

tasks.register<Exec>("runDebugOnEmulatorNoSandbox") {
    group = "Aurora Devices"
    description = "Install Debug to Emulator and launch it with runtime-manager-tool --nosandbox --detach."
    dependsOn("killAuroraDemoOnEmulator", "installDebugToEmulator")
    notCompatibleWithConfigurationCache("Uses local SSH exec calls to launch the Aurora app outside the standard plugin flow.")
    commandLine(
        "ssh",
        "-o", "StrictHostKeyChecking=no",
        "-o", "UserKnownHostsFile=/dev/null",
        "-o", "LogLevel=ERROR",
        "-p", auroraPort,
        "-i", auroraSshKey,
        "$auroraUser@$auroraHost",
        "runtime-manager-tool Control startDebug $auroraAppId --nosandbox --detach --output-to-console",
    )
}

tasks.register<Exec>("runDebugOnEmulatorNoSandboxStreaming") {
    group = "Aurora Devices"
    description = "Install Debug to Emulator and launch it with runtime-manager-tool --nosandbox without detach, streaming stdout/stderr to outputs/aurora_runtime_streaming.log. Kills stale emulator-side app processes first."
    dependsOn("killAuroraDemoOnEmulator", "installDebugToEmulator")
    notCompatibleWithConfigurationCache("Uses local SSH exec calls and local log capture for Aurora runtime streaming.")

    doFirst {
        project.rootProject.file("outputs").mkdirs()
    }

    commandLine(
        "bash",
        "-lc",
        """
        ssh -tt \
          -o StrictHostKeyChecking=no \
          -o UserKnownHostsFile=/dev/null \
          -o LogLevel=ERROR \
          -p $auroraPort \
          -i $auroraSshKey \
          $auroraUser@$auroraHost \
          'runtime-manager-tool Control startDebug $auroraAppId --nosandbox --output-to-console' \
          2>&1 | tee ${project.rootProject.file("outputs/aurora_runtime_streaming.log").absolutePath}
        """.trimIndent(),
    )
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    applyDefaultHierarchyTemplate()

    listOf(
        linuxArm64("auroraArm64"),
        linuxX64("auroraX64"),
    ).forEach { target ->
        target.binaries {
            executable {
                entryPoint = "com.example.aurorakmpdemo.main"
            }
            all {
                freeCompilerArgs += auroraBuild.freeCompilerArgs(target.name)
                linkerOpts += auroraBuild.cmpLinkerOpts(target.name)
                linkerOpts += auroraBuild.kmpLinkerOpts(
                    targetName = target.name,
                    "appdir",
                    "Qt5Core",
                    "Qt5Network",
                )
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(project.dependencies.platform("io.insert-koin:koin-bom:4.2.0-aurora"))
                implementation("io.insert-koin:koin-core")
                implementation("io.insert-koin:koin-compose")
                implementation("io.insert-koin:koin-compose-viewmodel")
                implementation(project.dependencies.platform("io.ktor:ktor-bom:3.4.2-aurora"))
                implementation("io.ktor:ktor-client-core")
                implementation("io.ktor:ktor-client-curl")
                implementation("io.ktor:ktor-client-content-negotiation")
                implementation("io.ktor:ktor-serialization-kotlinx-json")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-aurora")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
                implementation("androidx.room:room-runtime:2.8.4")
                implementation("androidx.sqlite:sqlite-bundled:2.6.2")
            }
        }

        val auroraMain by creating {
            dependsOn(commonMain)
        }

        val auroraArm64Main by getting {
            dependsOn(auroraMain)
        }
        val auroraX64Main by getting {
            dependsOn(auroraMain)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", "androidx.room:room-compiler:2.8.4")
    add("kspAuroraArm64", "androidx.room:room-compiler:2.8.4")
    add("kspAuroraX64", "androidx.room:room-compiler:2.8.4")
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.example.aurorakmpdemo.resources"
}

room {
    schemaDirectory("$projectDir/schemas")
}
