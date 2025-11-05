pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral() // This is important for OpenCV
        maven { url = uri("https://jitpack.io") } // You have jitpack listed twice, which is harmless but redundant
        // maven { url = uri("https://jitpack.io") } // This is a duplicate
        maven{ url = uri("https://pkgs.dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging/Duo-SDK-Feed/maven/v1")}
        }
}


rootProject.name = "Business Card Scanner App"
include(":app")
