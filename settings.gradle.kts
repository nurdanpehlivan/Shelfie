pluginManagement {
    // Convention plugins shared by every module (shelfie.android.library, .feature, ...).
    includeBuild("build-logic")
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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Shelfie"
include(":app")

include(":core:domain")
include(":core:data")
include(":core:ui")

include(":feature:pantry")
include(":feature:scanner")
include(":feature:recipe")
include(":feature:mealplan")
include(":feature:shopping")
include(":feature:statistics")
include(":feature:settings")
include(":feature:onboarding")
 