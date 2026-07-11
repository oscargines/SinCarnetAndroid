import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

@Suppress("DEPRECATION")
android {
    namespace = "com.oscar.sincarnet.presentation"
    compileSdk = 35

    defaultConfig {
        minSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    val iosTargets = listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64()
    )
    iosTargets.forEach { target ->
        (target as KotlinNativeTarget).binaries.framework {
            baseName = "SinCarnetShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(kotlin("stdlib"))
        }

        androidMain.dependencies {
            implementation(project(":data"))

            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.activity.compose)
            implementation("androidx.compose.ui:ui:1.7.0")
            implementation("androidx.compose.ui:ui-graphics:1.7.0")
            implementation("androidx.compose.ui:ui-tooling-preview:1.7.0")
            implementation("androidx.compose.material3:material3:1.3.0")
            implementation("org.jetbrains.kotlin:kotlin-reflect")
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
            implementation(libs.androidx.navigation.compose)
        }

        iosMain.dependencies {
            implementation(project(":data"))
        }

        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation("org.json:json:20230227")
        }
    }
}

dependencies {
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.0")
}
