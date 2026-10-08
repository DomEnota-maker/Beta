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

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    sourceSets {
        getByName("main").assets.srcDir("../content-packs")
    }
}

dependencies {
    implementation(project(":assistant-core"))
    implementation(project(":design-system"))
    implementation(project(":feature-acceptance"))
    implementation(project(":feature-safety"))
    implementation(project(":domain-contracts"))
    implementation(project(":content-runtime"))
    implementation(project(":navigation-contracts"))
    implementation(project(":link-router"))
    implementation(project(":source-policy"))

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
