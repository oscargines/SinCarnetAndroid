import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    kotlin("android")
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.dokka")
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")

if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    namespace = "com.oscar.sincarnet"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.oscar.sincarnet"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.31.00 "

        // Mitigacion temporal Android 16KB page size:
        // evitamos empaquetar binarios x86/x86_64 (los que estan reportando
        // falta de alineacion) en APKs para dispositivos ARM reales.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
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
            // Evitar conflictos por firmas de JARs de BouncyCastle y otras librerías
            excludes += "META-INF/*.SF"
            excludes += "META-INF/*.DSA"
            excludes += "META-INF/versions/9/**"
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":presentation"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.androidx.navigation.compose)
    // Fused Location Provider
    testImplementation(libs.junit)
    // Añadido para disponer de org.json en tests JVM (evita errores de métodos "not mocked" en JSONObject/JSONArray)
    testImplementation("org.json:json:20230227")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.10")
    // lifecycle-runtime-compose: LocalLifecycleOwner actualizado (no deprecado)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

}

// Configuración Dokka V2 (plugin mode V2EnabledWithHelpers en gradle.properties)
dokka {
    dokkaSourceSets.configureEach {
        displayName.set("appMain")
        sourceRoots.from(file("src/main/java"))
        sourceRoots.from(file("src/main/kotlin"))
        suppress.set(false)
        documentedVisibilities.set(
            setOf(
                org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Public,
                org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Internal,
                org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Protected,
                org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Private
            )
        )
        skipDeprecated.set(false)
        reportUndocumented.set(false)
    }
}

tasks.register<Sync>("publishDokkaToDocs") {
    group = "documentation"
    description = "Genera Dokka HTML y publica la salida versionable en docs/api"
    dependsOn("dokkaGeneratePublicationHtml")
    from(layout.buildDirectory.dir("dokka/html"))
    into(rootProject.layout.projectDirectory.dir("docs/api"))
}

