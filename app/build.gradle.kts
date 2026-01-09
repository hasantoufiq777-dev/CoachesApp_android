plugins{
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")  // Google Services plugin for Firebase
}

android {
    namespace = "com.example.coachesapp_android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.coachesapp_android"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    // Core libraries desugaring for LocalDateTime support on API < 26
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // RecyclerView for PlayerListActivity
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    // Firebase BOM (Bill of Materials) - manages all Firebase versions
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-auth")          // Authentication
    implementation("com.google.firebase:firebase-firestore")     // Cloud Firestore
    implementation("com.google.firebase:firebase-storage")       // Cloud Storage
    implementation("com.google.firebase:firebase-analytics")     // Analytics (optional)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}