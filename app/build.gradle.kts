import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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
        versionName = "1.21.5"

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

dependencies {
    // Excluir JARs de BouncyCastle "plataforma Android" (versiones recortadas 1.5x.0.0).
    // Estos no contienen DERObjectIdentifier y colisionan con el Maven Central BC completo.
    // También se excluye jmulticard-2.0.jar (su contenido ya está en dniedroid-release.aar).
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Fused Location Provider
    implementation("com.google.android.gms:play-services-location:21.0.1")
    testImplementation(libs.junit)
    // Añadido para disponer de org.json en tests JVM (evita errores de métodos "not mocked" en JSONObject/JSONArray)
    testImplementation("org.json:json:20230227")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // BouncyCastle: proveedor de criptografía requerido por dniedroid/jmulticard (org.bouncycastle.*)
    // Android 12+ (API 31) ya no incluye BouncyCastle en la plataforma; hay que empaquetar
    // las dos dependencias: proveedor + módulo PKIX/CMS.
    // IMPORTANTE: usar 1.50 (jdk15on). El AAR dniedroid usa clases ASN.1 antiguas (p.ej.
    // DERObjectIdentifier) que fueron eliminadas a partir de BC 1.70. Los JARs locales
    // prov/pkix/core-1.5x.0.0 son versiones Android recortadas y están excluidos del fileTree.
    implementation("org.bouncycastle:bcprov-jdk15on:1.50")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.50")

}
