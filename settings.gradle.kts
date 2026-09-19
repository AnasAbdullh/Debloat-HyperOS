pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Shizuku artifacts
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "DebloatHyperOS"
include(":app")
