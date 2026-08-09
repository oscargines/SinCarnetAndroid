plugins {
    kotlin("multiplatform")
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

@Suppress("DEPRECATION")
android {
    namespace = "com.oscar.sincarnet.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 31
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/FastDoubleParser-**"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/*.SF"
            excludes += "META-INF/*.DSA"
            excludes += "META-INF/versions/9/**"
            excludes += "org/apache/commons/validator/resources/validator_1_1.dtd"
            excludes += "org/apache/commons/validator/resources/validator_1_0.dtd"
        }
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(kotlin("stdlib"))
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation("androidx.compose.ui:ui:1.7.6")
            implementation("androidx.compose.ui:ui-graphics:1.7.6")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

            // Excluir JARs de BouncyCastle "plataforma Android" (versiones recortadas 1.5x.0.0).
            implementation(fileTree("libs") {
                include("*.jar")
                exclude(
                    "jmulticard-2.0.jar",
                    "prov-1.58.0.0.jar",
                    "pkix-1.54.0.0.jar",
                    "core-1.58.0.0.jar"
                )
            })
            implementation(files("libs/dniedroid-release.aar"))

            // BouncyCastle: proveedor de criptografía requerido por dniedroid/jmulticard
            // Actualizado de 1.50 a 1.78.1 para corregir CVEs conocidos
            implementation("org.bouncycastle:bcprov-jdk15on:1.78.1")
            implementation("org.bouncycastle:bcpkix-jdk15on:1.78.1")

            // CameraX (api para que las screens en :app puedan usar los tipos directamente)
            api("androidx.camera:camera-core:1.4.1")
            api("androidx.camera:camera-camera2:1.4.1")
            api("androidx.camera:camera-lifecycle:1.4.1")
            api("androidx.camera:camera-view:1.4.1")
            api("androidx.exifinterface:exifinterface:1.4.1")

            // Fused Location Provider (api para que las screens en :app puedan usar LocationServices)
            api("com.google.android.gms:play-services-location:21.3.0")
        }

        iosMain.dependencies {
            // Foundation para NSUserDefaults y otras APIs de iOS base
        }

        androidUnitTest.dependencies {
            implementation("org.json:json:20230227")
            implementation(libs.junit)
        }
    }
}
