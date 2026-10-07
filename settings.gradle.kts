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
    }
}

rootProject.name = "RailBrakeCalculator"

include(
    ":app-shell",
    ":assistant-core",
    ":design-system",
    ":domain-contracts",
    ":content-runtime",
    ":navigation-contracts",
    ":link-router",
    ":source-policy",
)
