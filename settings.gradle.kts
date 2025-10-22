// In C:/Users/PC-1/ChickCare-Thesis/ChickCare/settings.gradle.kts

pluginManagement {
    repositories {google {
        content {
            includeGroupByRegex("com\\.android.*")
            includeGroupByRegex("com\\.google.*")
            includeGroupByRegex("androidx.*")
        }
    }
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // This line is also correct, keep it.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ChickCare"
include(":app")
