plugins {
    kotlin("multiplatform") version "2.3.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
    id("com.google.devtools.ksp") version "2.3.9"
    id("androidx.room") version "2.8.4"
    id("ru.auroraos.kmp.aurora-build") version "0.0.1"
    id("ru.auroraos.kmp.aurora-devices") version "0.0.1"
    id("org.jetbrains.compose") version "0.0.5-aurora"
}

version = "0.0.3"

val auroraNavigationVersion = "9999.0.0-SNAPSHOT"
// Для установки ConnMan plugin и запроса VPN capability нужен пакет с
// extended-подписью. Development/debug-сборка остаётся устанавливаемой, пока
// signing profile не будет передан явно.
val includeSystemVpnPayload = providers.gradleProperty("aurora.systemVpnIntegration")
    .map(String::toBoolean)
    .getOrElse(false)

auroraBuild {
    targets {
        arm64.set("auroraArm64")
        x64.set("auroraX64")
    }
    rpm {
        id.set("com.example.aurorakmpdemo")
        name.set("Aurora KMP Demo")
        version.set(project.version.toString())
        description.set("Aurora Kotlin Multiplatform compatibility check.")
        // Встроенный development-шаг подписи plugin сейчас создаёт RPM, у
        // которого не проходит проверка digest payload. Здесь собирается
        // неподписанный RPM с корректными контрольными суммами; release-подпись
        // выполняется отдельно доверенным процессом после этой задачи.
        sign.set(false)
        icons.set(projectDir.toPath().resolve("icons"))
        libs3rdParty.set(listOf("maliit-glib"))
        permissions.set(buildList {
            add("Internet")
            add("UserDirs")
            if (includeSystemVpnPayload) add("VPN")
        })
        if (includeSystemVpnPayload) {
            // Test VPN намеренно работает только как split-tunnel: он обслуживает
            // 198.51.100.0/24, не меняет default route и DNS, а provider
            // отбрасывает кадры. Для этой ветки нужна extended-подпись.
            extraFiles {
                from { arch -> projectDir.resolve("native-vpn/connman-test-plugin/build/${arch.key}/plugin/libaurora-test-route-proto-plugin.so") } into
                    "/usr/lib/connman/plugins-vpn/aurora-test-route-proto-plugin.so" mode "0755"
                // Путь должен совпадать с PROVIDER_PATH из split_tunnel_plugin.c.
                // ConnMan сам запускает provider.
                from { arch -> projectDir.resolve("native-vpn/connman-test-plugin/build/${arch.key}/provider/com.example.aurorakmpdemo.test-route-vpn") } into
                    "/usr/bin/com.example.aurorakmpdemo.test-route-vpn" mode "0755"
                from(projectDir.resolve("native-vpn/connman-test-plugin/provider/aurora-test-route.conf")) into
                    "/etc/connman/vpn-plugin/aurora-test-route.conf" mode "0644"
            }
        }
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
val auroraSshKey = providers.gradleProperty("aurora.sshKey")
    .orElse(providers.environmentVariable("AURORA_SSH_KEY"))
    .orElse("${System.getProperty("user.home")}/AuroraOS/vmshare/ssh/private_keys/sdk")
    .get()
val auroraDrawableExperiment = providers.gradleProperty("aurora.drawableExperiment")
    .orElse(providers.environmentVariable("AURORA_DRAWABLE_EXPERIMENT"))
    .orNull
val auroraDrawableExperimentFlag = auroraDrawableExperiment
    ?.let { " -e 'AURORA_DRAWABLE_EXPERIMENT=$it'" }
    .orEmpty()
val auroraAppId = "com.example.aurorakmpdemo"

tasks.register<Delete>("purgeAuroraMacMetadata") {
    group = "Aurora Devices"
    description = "Delete macOS metadata files that break Aurora RPM packaging."
    delete(fileTree(projectDir) { include("**/.DS_Store") })
    delete(fileTree(layout.buildDirectory) { include("**/.DS_Store") })
}

// Здесь только исходники. Созданные RPM, Makefile и бинарники находятся в
// build/ или .rpmbuild/ и не должны попадать в Git как исходный код.
val nativeVpnDirectory = projectDir.resolve("native-vpn/connman-test-plugin")
val auroraSdkDirectory = file("${System.getProperty("user.home")}/AuroraOS/sdk/5.2.0.180")
val systemVpnRpmDirectory = projectDir.resolve("system-rpm")

tasks.register<Exec>("buildNativeTestVpnProviderAuroraArm64") {
    group = "Aurora VPN"
    description = "Builds the aarch64 ConnMan split-tunnel test plugin and its packet-sink provider."
    notCompatibleWithConfigurationCache("Invokes the Aurora SDK Docker build and recreates its generated qmake directory.")
    workingDir(nativeVpnDirectory.resolve("build/aarch64"))
    // plugin реализует ABI ConnMan, provider — приёмник пакетов TUN, bridge — узкий D-Bus API.
    inputs.dir(nativeVpnDirectory.resolve("plugin"))
    inputs.dir(nativeVpnDirectory.resolve("provider"))
    inputs.file(nativeVpnDirectory.resolve("connman-test-plugin.pro"))
    inputs.file(nativeVpnDirectory.resolve("aurora-kmp-test-route-vpn.spec"))
    inputs.file(nativeVpnDirectory.resolve("build-aarch64.sh"))
    outputs.file(nativeVpnDirectory.resolve("build/aarch64/plugin/libaurora-test-route-proto-plugin.so"))
    outputs.file(nativeVpnDirectory.resolve("build/aarch64/provider/aurora-test-route-provider"))
    outputs.file(nativeVpnDirectory.resolve("build/aarch64/bridge/com.example.aurorakmpdemo-vpn-bridge"))
    // Обёртка Aurora qmake переиспользует созданные Makefile и иначе может не
    // заметить изменение только C-исходника внутри Docker build-директории.
    outputs.upToDateWhen { false }
    doFirst {
        delete(nativeVpnDirectory.resolve("build/aarch64/plugin"))
        delete(nativeVpnDirectory.resolve("build/aarch64/provider"))
        nativeVpnDirectory.resolve("build/aarch64").mkdirs()
    }
    environment("WORKSPACE_DIR", rootProject.projectDir.absolutePath)
    commandLine(
        "bash",
        nativeVpnDirectory.resolve("build-aarch64.sh").absolutePath,
    )
}

tasks.register<Exec>("buildSystemVpnBundleAuroraArm64") {
    group = "Aurora VPN"
    description = "Builds a development-only combined KMP application + ConnMan VPN RPM using the custom system spec."
    // Зависимости обеспечивают настоящую сборку одного RPM: до staging заново
    // собираются KMP executable и все нативные VPN-компоненты.
    dependsOn("buildDebugPackageAuroraArm64", "buildNativeTestVpnProviderAuroraArm64")
    notCompatibleWithConfigurationCache("Stages KMP and native artifacts, then invokes rpmbuild in the Aurora SDK Docker image.")
    workingDir(systemVpnRpmDirectory)
    inputs.dir(projectDir.resolve("build/rpm/debug/aarch64"))
    inputs.file(nativeVpnDirectory.resolve("build/aarch64/plugin/libaurora-test-route-proto-plugin.so"))
    inputs.file(nativeVpnDirectory.resolve("build/aarch64/provider/com.example.aurorakmpdemo.test-route-vpn"))
    inputs.file(nativeVpnDirectory.resolve("build/aarch64/bridge/com.example.aurorakmpdemo-vpn-bridge"))
    inputs.file(systemVpnRpmDirectory.resolve("aurora-kmp-demo-system.spec"))
    inputs.file(systemVpnRpmDirectory.resolve("build-aarch64.sh"))
    outputs.file(systemVpnRpmDirectory.resolve(".rpmbuild/aarch64/RPMS/aarch64/com.example.aurorakmpdemo-0.0.3-1.aarch64.rpm"))
    environment("WORKSPACE_DIR", rootProject.projectDir.absolutePath)
    environment("AURORA_SDK_DIR", auroraSdkDirectory.absolutePath)
    commandLine("bash", systemVpnRpmDirectory.resolve("build-aarch64.sh").absolutePath)
}

tasks.register<Exec>("killAuroraDemoOnEmulator") {
    group = "Aurora Devices"
    description = "Stops any running Aurora KMP Demo processes inside the emulator before the next launch."
    notCompatibleWithConfigurationCache("Uses local SSH exec calls to kill emulator-side app processes.")
    isIgnoreExitValue = true
    commandLine(
        "ssh",
        "-o", "StrictHostKeyChecking=no",
        "-o", "UserKnownHostsFile=/dev/null",
        "-o", "LogLevel=ERROR",
        "-p", auroraPort,
        "-i", auroraSshKey,
        "$auroraUser@$auroraHost",
        "sh", "-c",
        "cd /home/$auroraUser || exit 0; pkill -9 -f '$auroraAppId' 2>/dev/null || true; sleep 1",
    )
}

tasks.register<Exec>("runDebugOnEmulatorNoSandbox") {
    group = "Aurora Devices"
    description = "Install Debug to Emulator and keep a streaming no-sandbox debug session alive while the app runs."
    dependsOn("killAuroraDemoOnEmulator", "installDebugToEmulator")
    notCompatibleWithConfigurationCache("Uses local SSH exec calls to launch the Aurora app outside the standard plugin flow.")
    commandLine(
        "ssh",
        "-tt",
        "-o", "StrictHostKeyChecking=no",
        "-o", "UserKnownHostsFile=/dev/null",
        "-o", "LogLevel=ERROR",
        "-p", auroraPort,
        "-i", auroraSshKey,
        "$auroraUser@$auroraHost",
        // Aurora 5.2 останавливает debug-приложение при завершении сессии
        // runtime-manager. Псевдо-TTY и controller без detach соответствуют IDE-пути.
        "runtime-manager-tool Control startDebug $auroraAppId --nosandbox --output-to-console$auroraDrawableExperimentFlag",
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
          'runtime-manager-tool Control startDebug $auroraAppId --nosandbox --output-to-console$auroraDrawableExperimentFlag' \
          2>&1 | tee ${project.rootProject.file("outputs/aurora_runtime_streaming.log").absolutePath}
        """.trimIndent(),
    )
}

tasks.matching { it.name == "installDebugToEmulator" }.configureEach {
    // Подключённый Aurora emulator имеет aarch64; x86_64 не упаковывается при каждом запуске.
    dependsOn("buildDebugPackageAuroraArm64")
}

tasks.matching { it.name == "buildDebugPackageAuroraArm64" }.configureEach {
    // Packaging plugin копирует composeApp.kexe как есть; после изменения исходников его нужно перелинковать.
    dependsOn("linkDebugExecutableAuroraArm64", "buildNativeTestVpnProviderAuroraArm64")
}

tasks.matching {
    it.name in setOf(
        "buildDebugPackageAuroraArm64",
        "buildDebugPackageAuroraX64",
        "buildDebugPipeline",
        "installDebugToEmulator",
        "runDebugOnEmulator",
        "runDebugOnEmulatorNoSandbox",
        "runDebugOnEmulatorNoSandboxStreaming",
    )
}.configureEach {
    dependsOn("purgeAuroraMacMetadata")
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
                implementation(compose.components.resources)
                implementation("org.jetbrains.androidx.navigation:navigation-compose:$auroraNavigationVersion")
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
            dependencies {
                // KMP D-Bus binding Aurora. Реальный VPN-адаптер находится
                // только в auroraMain; общий UI сохраняет переносимый API controller.
                implementation("ru.auroraos.kmp:dbus-library:0.0.1")
            }
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

tasks.matching { it.name == "compileKotlinAuroraX64" }.configureEach {
    mustRunAfter("compileKotlinAuroraArm64")
}

tasks.matching { it.name == "linkDebugExecutableAuroraX64" }.configureEach {
    mustRunAfter("linkDebugExecutableAuroraArm64")
}

tasks.matching { it.name == "buildDebugPackageAuroraX64" }.configureEach {
    mustRunAfter("buildDebugPackageAuroraArm64")
}
