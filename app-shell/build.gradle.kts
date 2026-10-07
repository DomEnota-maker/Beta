plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ru.railbrake.calculator.shell"
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.railbrake.calculator"
        minSdk = 26
        targetSdk = 34
        versionCode = 204
        versionName = "1.2.2-modular-alpha01"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":assistant-core"))
    implementation(project(":domain-contracts"))
    implementation(project(":content-runtime"))
    implementation(project(":navigation-contracts"))
    implementation(project(":link-router"))
    implementation(project(":source-policy"))
}
