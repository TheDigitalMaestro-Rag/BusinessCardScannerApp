plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("androidx.room")

    id("com.google.gms.google-services")
}

android {
    namespace = "com.project.businesscardscannerapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.project.businesscardscannerapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appAuthRedirectScheme"] = "com.project.businesscardscannerapp" // Or any other unique scheme
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
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src\\main\\assets", "src\\main\\assets")
            }
        }
    }
    room {
        schemaDirectory("$projectDir/schemas")
    }
}

val room_version = "2.7.1"

kapt {
    correctErrorTypes = true
    arguments {
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.benchmark.traceprocessor.jvm)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation ("androidx.activity:activity-compose:1.8.2")
    implementation ("androidx.compose.ui:ui:1.6.6")
    implementation ("androidx.compose.ui:ui-tooling:1.6.6")
    implementation ("androidx.compose.foundation:foundation:1.6.6")
    implementation ("androidx.compose.material3:material3:1.2.1")
    implementation ("androidx.compose.runtime:runtime-livedata:1.6.6")
    implementation ("androidx.navigation:navigation-compose:2.9.0")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation ("androidx.compose.material:material:1.6.6")
    implementation ("androidx.compose.material:material-icons-extended:1.0.0")


    // Core dependencies
    implementation ("androidx.core:core-ktx:1.12.0")
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation ("com.google.android.material:material:1.11.0")

    // Compose dependencies
    implementation ("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.02"))
    implementation ("androidx.compose.ui:ui")
    implementation ("androidx.compose.ui:ui-graphics")
    implementation ("androidx.compose.ui:ui-tooling-preview")
    implementation ("androidx.compose.material3:material3")
    implementation ("androidx.compose.material:material-icons-extended")
    implementation ("androidx.compose.runtime:runtime-livedata")

    // Lifecycle components
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.9.0")


    implementation("com.google.dagger:hilt-android:2.56.2")
    kapt("com.google.dagger:hilt-android-compiler:2.56.2")

    implementation ("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("androidx.room:room-runtime:$room_version")

    // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
    // See Add the KSP plugin to your project
    kapt("androidx.room:room-compiler:$room_version")

    // If this project only uses Java source, use the Java annotationProcessor
    // No additional plugins are necessary
    annotationProcessor("androidx.room:room-compiler:$room_version")

    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$room_version")

    // optional - RxJava2 support for Room
    implementation("androidx.room:room-rxjava2:$room_version")

    // optional - RxJava3 support for Room
    implementation("androidx.room:room-rxjava3:$room_version")

    // optional - Guava support for Room, including Optional and ListenableFuture
    implementation("androidx.room:room-guava:$room_version")

    // optional - Test helpers
    testImplementation("androidx.room:room-testing:$room_version")

    // optional - Paging 3 Integration
    implementation("androidx.room:room-paging:$room_version")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")

    implementation("com.google.mlkit:text-recognition:16.0.0")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")



    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0")
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.0")
    implementation("com.google.mlkit:text-recognition-japanese:16.0.0")
    implementation("com.google.mlkit:text-recognition-korean:16.0.0")



    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

// Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

// Compose
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    implementation(libs.play.services.ads)

    implementation("com.journeyapps:zxing-android-embedded:4.3.0") // For BarcodeEncoder

    // For vCard generation
    implementation("com.googlecode.ez-vcard:ez-vcard:0.11.3")

    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

// Google Auth
    implementation("com.google.android.gms:play-services-auth:20.7.0")

// Microsoft Auth
    implementation("com.microsoft.identity.client:msal:7.0.3")

    implementation("androidx.work:work-runtime-ktx:2.9.0") // For WorkManager

    // Add to your existing dependencies
    implementation("com.google.firebase:firebase-auth:22.3.0")
    implementation("com.google.firebase:firebase-database:20.3.0")
    implementation("com.google.firebase:firebase-storage:20.3.0")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.zxing:core:3.5.2")

    // ML Kit Smart Reply
    implementation("com.google.mlkit:smart-reply:17.0.2")
    // Coroutines for async ML operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")
    // WorkManager for scheduled notifications
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    // DataStore for secure token storage
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Crypto for token encryption
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // For OAuth flow
    implementation("net.openid:appauth:0.11.1")
    implementation("androidx.browser:browser:1.6.0")

// In app/build.gradle.kts

    dependencies {
        // Core & UI
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.ui)
        implementation(libs.androidx.ui.graphics)
        implementation(libs.androidx.ui.tooling.preview)
        implementation(libs.androidx.material3)
        implementation("androidx.compose.material:material-icons-extended")
        implementation("androidx.navigation:navigation-compose:2.9.0")

        // Lifecycle
        implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
        implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

        // Hilt for Dependency Injection
        implementation("com.google.dagger:hilt-android:2.56.2")
        kapt("com.google.dagger:hilt-android-compiler:2.56.2")
        implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

        // Room for Database
        val room_version = "2.7.1"
        implementation("androidx.room:room-runtime:$room_version")
        implementation("androidx.room:room-ktx:$room_version")
        kapt("androidx.room:room-compiler:$room_version")
        testImplementation("androidx.room:room-testing:$room_version")

        // Networking (Retrofit & Moshi)
        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        implementation("com.squareup.retrofit2:converter-moshi:2.9.0") // Moshi converter for Retrofit
        implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
        implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
        kapt("com.squareup.moshi:moshi-kotlin-codegen:1.15.0") // Kapt for Moshi

        // Firebase
        implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
        implementation("com.google.firebase:firebase-auth-ktx")
        implementation("com.google.firebase:firebase-analytics-ktx")
        implementation("com.google.firebase:firebase-database")
        implementation("com.google.firebase:firebase-storage")
        implementation(libs.firebase.firestore.ktx)

        // Google Services (ML Kit, Auth, Ads)
        implementation("com.google.android.gms:play-services-auth:20.7.0")
        implementation(libs.play.services.ads)
        implementation("com.google.mlkit:text-recognition:16.0.0")
        implementation("com.google.mlkit:barcode-scanning:17.2.0")
        implementation("com.google.mlkit:smart-reply:17.0.2")

        // CameraX
        implementation("androidx.camera:camera-camera2:1.3.0")
        implementation("androidx.camera:camera-lifecycle:1.3.0")
        implementation("androidx.camera:camera-view:1.3.0")

        // Image Loading
        implementation("io.coil-kt:coil-compose:2.4.0")

        // Other Utilities
        implementation("com.google.accompanist:accompanist-permissions:0.32.0")
        implementation("com.journeyapps:zxing-android-embedded:4.3.0")
        implementation("com.googlecode.ez-vcard:ez-vcard:0.11.3")
        implementation("androidx.datastore:datastore-preferences:1.0.0")
        implementation("androidx.work:work-runtime-ktx:2.9.0")
        implementation("net.openid:appauth:0.11.1")

        // Coroutines
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")

        // Testing
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.ui.test.junit4)
        debugImplementation(libs.androidx.ui.tooling)
        debugImplementation(libs.androidx.ui.test.manifest)
    }

}
kapt {
    correctErrorTypes = true
}
