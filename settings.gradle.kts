@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenLocal()
        val buildVariant = providers.gradleProperty("buildVariant").orNull
        val auroraMavenRepo = java.io.File(settings.rootDir, "work/aurora-maven-partial")
            .takeIf { it.exists() }
        if (buildVariant == "aurora" && auroraMavenRepo != null) {
            maven(url = auroraMavenRepo.toURI())
        }
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
        mavenLocal()
        val buildVariant = providers.gradleProperty("buildVariant").orNull
        val auroraMavenRepo = java.io.File(settings.rootDir, "work/aurora-maven-partial")
            .takeIf { it.exists() }
        if (buildVariant == "aurora" && auroraMavenRepo != null) {
            maven(url = auroraMavenRepo.toURI())
        }
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
