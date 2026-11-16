@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        val buildVariant = providers.gradleProperty("buildVariant").orNull
        val auroraMavenRepo = providers.gradleProperty("auroraMavenDir").orNull
            ?.let { path -> java.io.File(path).let { if (it.isAbsolute) it else java.io.File(settings.rootDir, path) } }
            ?: java.io.File(settings.rootDir, "work/aurora-maven-0.0.6")
        val availableAuroraMavenRepo = auroraMavenRepo
            .takeIf { it.exists() }
        if (buildVariant == "aurora" && availableAuroraMavenRepo != null) {
            maven(url = availableAuroraMavenRepo.toURI())
        }
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        val buildVariant = providers.gradleProperty("buildVariant").orNull
        val auroraMavenRepo = providers.gradleProperty("auroraMavenDir").orNull
            ?.let { path -> java.io.File(path).let { if (it.isAbsolute) it else java.io.File(settings.rootDir, path) } }
            ?: java.io.File(settings.rootDir, "work/aurora-maven-0.0.6")
        val availableAuroraMavenRepo = auroraMavenRepo
            .takeIf { it.exists() }
        if (buildVariant == "aurora" && availableAuroraMavenRepo != null) {
            maven(url = availableAuroraMavenRepo.toURI())
        }
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
    }
}

rootProject.name = "aurora-kmp-compose-check"

include(":composeApp")
project(":composeApp").apply {
    projectDir = file("composeApp")
    buildFileName = when (providers.gradleProperty("buildVariant").orNull) {
        "aurora" -> "build.aurora.gradle.kts"
        else -> "build.gradle.kts"
    }
}
