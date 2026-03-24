plugins {
    alias(libs.plugins.android.application)
<<<<<<< HEAD
    // ⭐ alias 대신 직접 id를 쓰고, 뒤에 apply false를 뺍니다.
    // (Project 수준의 gradle에서 이미 정의되어 있다면 아래처럼만 쓰면 됩니다)
=======
>>>>>>> ca178ecdf5d6d6fc1948225597641ad0fce1c061
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.bugzero.meety"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bugzero.meety"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
<<<<<<< HEAD

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
=======
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
>>>>>>> ca178ecdf5d6d6fc1948225597641ad0fce1c061
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
<<<<<<< HEAD

=======
>>>>>>> ca178ecdf5d6d6fc1948225597641ad0fce1c061
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
<<<<<<< HEAD

    // ⭐ 이 부분을 jvmTarget = "11" 대신 아래처럼 써보세요 (더 강력한 선언입니다)
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

=======
>>>>>>> ca178ecdf5d6d6fc1948225597641ad0fce1c061
    buildFeatures {
        compose = true
    }
}

dependencies {
<<<<<<< HEAD
    // 예원님이 쓰시던 내용 그대로 유지
=======
    implementation("com.airbnb.android:lottie-compose:6.1.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)
>>>>>>> ca178ecdf5d6d6fc1948225597641ad0fce1c061
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
<<<<<<< HEAD
    implementation(libs.androidx.navigation.compose)
// Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
=======
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    // Firebase

    implementation(platform("com.google.firebase:firebase-bom:34.10.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.firebase:firebase-storage")

    // FCM (푸시 알림)
    implementation("com.google.firebase:firebase-messaging")
>>>>>>> ca178ecdf5d6d6fc1948225597641ad0fce1c061

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}