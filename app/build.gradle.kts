plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.m4xtheme.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.aistudio.m4xtheme.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 400
        versionName = "4.0.0"
        buildConfigField("String", "WEB_APP_URL", "\"https://ngmingzan.github.io/M4X-APP-NEW/\"")
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
}
