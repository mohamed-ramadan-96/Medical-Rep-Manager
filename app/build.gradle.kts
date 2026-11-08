plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.medicalrep.manager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.medicalrep.manager"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    val stableKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
    val stableStorePassword = System.getenv("ANDROID_STORE_PASSWORD")
    val stableKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
    val stableKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")

    signingConfigs {
        if (
            !stableKeystorePath.isNullOrBlank() &&
            !stableStorePassword.isNullOrBlank() &&
            !stableKeyAlias.isNullOrBlank() &&
            !stableKeyPassword.isNullOrBlank()
        ) {
            create("stable") {
                storeFile = file(stableKeystorePath)
                storePassword = stableStorePassword
                keyAlias = stableKeyAlias
                keyPassword = stableKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            if (signingConfigs.findByName("stable") != null) {
                signingConfig = signingConfigs.getByName("stable")
            }
        }
        release {
            signingConfig = signingConfigs.getByName("stable")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.navigation:navigation-compose:2.9.5")
    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("com.google.android.gms:play-services-auth:22.0.0")
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    // Local-first database
    implementation("androidx.room:room-runtime:2.8.3")
    implementation("androidx.room:room-ktx:2.8.3")
    kapt("androidx.room:room-compiler:2.8.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
