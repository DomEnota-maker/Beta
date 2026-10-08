plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

kotlin {
    jvmToolchain(17)
    sourceSets {
        main {
            kotlin.srcDir(rootProject.layout.projectDirectory.dir("build/vl80s-diagnostic-donor-src"))
        }
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
}

application {
    mainClass.set("ru.railbrake.calculator.export.Vl80sDiagnosticRuntimeExporterKt")
}
