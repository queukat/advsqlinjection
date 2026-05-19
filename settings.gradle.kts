@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://plugins.jetbrains.com/maven")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://plugins.jetbrains.com/maven")
    }
}

rootProject.name = "advsqlinjection"
