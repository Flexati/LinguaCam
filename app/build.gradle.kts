plugins {
    id("com.android.application") version "8.2.0" // INFERRED - Updated to 8.2.0 for compatibility with compileSdk 34
    id("org.jetbrains.kotlin.android") version "1.9.20" // INFERRED - Updated to 1.9.20 for compatibility with AGP 8.2.0
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20" // INFERRED - Updated to 1.9.20 for consistency
}

android {
    namespace = "com.linguacam"
    compileSdk = 34 // INFERRED

    defaultConfig {
        applicationId = "com.linguacam"
        minSdk = 24 // INFERRED
        targetSdk = 34 // INFERRED
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Step 5: ABI enforcement 64-bit come richiesto da Google Play.
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    // Step 5: signingConfigs.release legge da ~/.linguacam/keystore.properties (fuori dal repo).
    // Il file NON viene committato. Vedi docs/RELEASE.md.
    signingConfigs {
        create("release") {
            val keystoreProps = loadKeystoreProperties()
            if (keystoreProps != null) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    // ============================================================
    // Step 5 + 5-bis — funzione di lettura keystore properties
    // 1) Locale dev: ~/.linguacam/keystore.properties
    // 2) CI mode: env-vars LINGUACAM_KEYSTORE_FILE + KEYSTORE_PASSWORD + KEY_ALIAS + KEY_PASSWORD
    // 3) Nessuna config: ritorna null — build debug OK, release senza firma (warn, non blocca)
    // ============================================================
    fun loadKeystoreProperties(): java.util.Properties? {
        val keystoreFile = file("${System.getProperty("user.home")}/.linguacam/keystore.properties")
        if (keystoreFile.exists()) {
            val props = java.util.Properties()
            keystoreFile.inputStream().use { props.load(it) }
            return props
        }
        val envStoreFile = System.getenv("LINGUACAM_KEYSTORE_FILE")
        val envStorePassword = System.getenv("LINGUACAM_KEYSTORE_PASSWORD")
        val envKeyAlias = System.getenv("LINGUACAM_KEY_ALIAS")
        val envKeyPassword = System.getenv("LINGUACAM_KEY_PASSWORD")
        if (envStoreFile != null && envStorePassword != null && envKeyAlias != null && envKeyPassword != null) {
            val envProps = java.util.Properties()
            envProps.setProperty("storeFile", envStoreFile)
            envProps.setProperty("storePassword", envStorePassword)
            envProps.setProperty("keyAlias", envKeyAlias)
            envProps.setProperty("keyPassword", envKeyPassword)
            return envProps
        }
        return null
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5" // INFERRED - Updated for Kotlin 1.9.20 compatibility
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1") // INFERRED
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1") // INFERRED
    implementation("androidx.activity:activity-compose:1.7.2") // INFERRED
    implementation(platform("androidx.compose:compose-bom:2023.10.01")) // INFERRED - Updated for compatibility
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.1.1") // INFERRED
    implementation("androidx.compose.foundation:foundation:1.5.1") // VERIFIED - Added to resolve isSystemInDarkMode
    implementation("androidx.compose.material:material-icons-extended") // INFERRED - Added to resolve missing icon references

    // CameraX
    implementation("androidx.camera:camera-core:1.3.3") // INFERRED
    implementation("androidx.camera:camera-camera2:1.3.3") // INFERRED
    implementation("androidx.camera:camera-lifecycle:1.3.3") // INFERRED
    implementation("androidx.camera:camera-view:1.3.3") // INFERRED

    // ML Kit
    implementation("com.google.mlkit:text-recognition:16.0.0") // INFERRED - LATIN
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0") // Step 4 - ZH
    implementation("com.google.mlkit:text-recognition-japanese:16.0.0") // Step 4 - JA
    implementation("com.google.mlkit:translate:17.0.0") // INFERRED
    implementation("com.google.android.gms:play-services-tasks:18.0.2") // INFERRED

    // Google Play Billing
    implementation("com.android.billingclient:billing-ktx:6.2.1")

    // Timber for logging
    implementation("com.jakewharton.timber:timber:5.0.1") // INFERRED

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // INFERRED
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") // INFERRED

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0") // INFERRED

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0") // INFERRED

    testImplementation("junit:junit:4.13.2") // INFERRED
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5") // INFERRED
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // INFERRED
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01")) // INFERRED
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
