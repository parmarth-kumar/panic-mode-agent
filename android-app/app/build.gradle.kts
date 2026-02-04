plugins {
    // Core Android + Kotlin setup
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Jetpack Compose support
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.panicmode"
    compileSdk = 34 // Android 14 target (required for exact alarms & foreground rules)

    defaultConfig {
        applicationId = "com.panicmode"
        minSdk = 26          // Required for WorkManager + modern background limits
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Disabled for demo/hackathon builds to preserve stack traces
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Java 8 compatibility for Play Services + WorkManager
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    // Enable Jetpack Compose
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core Android support
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    // Background execution & scheduling
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Location services for live + fallback tracking
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Network layer (Mobilerun API)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.12.0")

    // Icon pack used across Compose UI
    implementation("br.com.devsrsouza.compose.icons:tabler-icons:1.1.0")

    // Jetpack core + lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Compose stack (BOM-managed)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
}
