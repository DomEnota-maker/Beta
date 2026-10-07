plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":domain-contracts"))
    implementation("com.google.code.gson:gson:2.11.0")
    testImplementation("junit:junit:4.13.2")
}
