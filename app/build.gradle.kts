plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.masjidku.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.masjidku.tv"
        minSdk = 21 // Android 5.0 ke atas (TV lama tetap bisa)
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // Kunci tanda tangan dari GitHub Secrets (KEYSTORE_BASE64, KEYSTORE_PASSWORD).
    // Bila secret belum diisi, APK ditandatangani kunci debug (tetap bisa dipasang).
    val releaseKeystore = System.getenv("KEYSTORE_FILE")
    signingConfigs {
        if (releaseKeystore != null) {
            create("rilis") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS") ?: "masjidku"
                keyPassword = System.getenv("KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (releaseKeystore != null) signingConfigs.getByName("rilis") else signingConfigs.getByName("debug")
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
        buildConfig = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Server web kecil untuk panel pengaturan dari HP
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    // Kode QR alamat panel di layar TV
    implementation("com.google.zxing:core:3.5.3")
}
