pluginManagement {
    plugins {
        id("com.android.application") version "8.7.3" // <= Hạ xuống từ 8.7.3
        id("com.google.gms.google-services") version "4.4.0" // ví dụ
    }
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "HomeRent"
include(":app")
 