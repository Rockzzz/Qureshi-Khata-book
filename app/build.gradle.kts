plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.tabelahisabapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tabelahisabapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Gemini API Key for voice parsing
        buildConfigField("String", "GEMINI_API_KEY", "\"AIzaSyAvWci4S7GO58xGht9u-plhLcqvFweqMNM\"")
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
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/notice.txt"
            excludes += "/META-INF/ASL2.0"
            excludes += "/META-INF/*.kotlin_module"
        }
    }
}

dependencies {
    // Core & Compose
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // Material Icons Extended (for CalendarToday, TrendingUp, etc.)
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Hilt for Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    
    // Gemini AI for voice parsing
    implementation("com.google.ai.client.generativeai:generativeai:0.1.2")
    
    // Gson for JSON backup serialization
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Google Sign-In for Google Drive backup
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    // Google Drive API
    implementation("com.google.api-client:google-api-client-android:2.2.0") {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation("com.google.apis:google-api-services-drive:v3-rev20231128-2.0.0") {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation("com.google.http-client:google-http-client-gson:1.43.3")
    
    // Coroutines Play Services for Tasks.await
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    correctErrorTypes = true
}
