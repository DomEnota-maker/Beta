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
    ":feature-acceptance",
    ":feature-atlas",
    ":feature-calculations",
    ":feature-diagnostics",
    ":feature-history",
    ":domain-contracts",
    ":content-runtime",
    ":navigation-contracts",
    ":link-router",
    ":source-policy",
)


include(":qa-vl80s-diagnostic-export")
project(":qa-vl80s-diagnostic-export").projectDir =
    file("qa-tools/vl80s-diagnostic-export")


include(":qa-vl80s-observation-export")
project(":qa-vl80s-observation-export").projectDir =
    file("qa-tools/vl80s-observation-export")
