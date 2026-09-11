plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            // Sin dependencias de plataforma; modelos puros de Kotlin.
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
