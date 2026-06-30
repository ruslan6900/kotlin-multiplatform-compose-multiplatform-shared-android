import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") version "2.3.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
    id("com.google.devtools.ksp") version "2.3.9"
    id("androidx.room") version "2.8.4"
    id("com.android.application") version "8.13.2"
    id("org.jetbrains.compose") version "1.9.3"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    applyDefaultHierarchyTemplate()

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
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
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.9.4")
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
                implementation(project.dependencies.platform("io.insert-koin:koin-bom:4.2.0"))
                implementation("io.insert-koin:koin-core")
                implementation("io.insert-koin:koin-compose")
                implementation("io.insert-koin:koin-compose-viewmodel")
                implementation(project.dependencies.platform("io.ktor:ktor-bom:3.4.2"))
                implementation("io.ktor:ktor-client-core")
                implementation("io.ktor:ktor-client-content-negotiation")
                implementation("io.ktor:ktor-serialization-kotlinx-json")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
                implementation("androidx.room:room-runtime:2.8.4")
                implementation("androidx.sqlite:sqlite-bundled:2.6.2")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation(compose.uiTooling)
                implementation("androidx.activity:activity-compose:1.10.1")
                implementation("io.ktor:ktor-client-okhttp")
            }
        }

        val iosMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-darwin")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("io.ktor:ktor-client-java")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
            }
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", "androidx.room:room-compiler:2.8.4")
    add("kspAndroid", "androidx.room:room-compiler:2.8.4")
    add("kspDesktop", "androidx.room:room-compiler:2.8.4")
    add("kspIosArm64", "androidx.room:room-compiler:2.8.4")
    add("kspIosSimulatorArm64", "androidx.room:room-compiler:2.8.4")
}

android {
    namespace = "com.example.aurorakmpdemo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.aurorakmpdemo"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

compose.desktop {
    application {
        mainClass = "com.example.aurorakmpdemo.MainKt"
        jvmArgs += listOf(
            "-Dskiko.renderApi=SOFTWARE",
        )
    }
}

tasks.matching { it.name == "desktopRun" || it.name == "run" }.configureEach {
    if (this is JavaExec) {
        jvmArgs("-Dskiko.renderApi=SOFTWARE")
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.example.aurorakmpdemo.resources"
}

room {
    schemaDirectory("$projectDir/schemas")
}
