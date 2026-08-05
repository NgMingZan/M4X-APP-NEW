import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val supabase = Properties().apply {
    val file = rootProject.file("supabase.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun quoted(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.m4xtheme.app"
    compileSdk = 35
    ndkVersion = "28.1.13356709"

    defaultConfig {
        applicationId = "com.aistudio.m4xtheme.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 74
        versionName = "4.1.1"

        ndk {
            abiFilters += listOf(
                "arm64-v8a",
                "armeabi-v7a",
                "x86_64"
            )
        }

        buildConfigField("String", "SUPABASE_URL", quoted(supabase.getProperty("SUPABASE_URL", "")))
        buildConfigField("String", "SUPABASE_ANON_KEY", quoted(supabase.getProperty("SUPABASE_ANON_KEY", "")))
        buildConfigField("String", "UPDATE_JSON_URL", quoted("https://ngmingzan.github.io/M4X-APP-NEW/update.json"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

// === M4X RUST THEME VALIDATOR ===
val buildRustThemeValidator by tasks.registering(Exec::class) {
    group = "rust"
    description = "Build Rust theme validator for Android ABIs"

    val rustManifest = rootProject.file(
        "rust/m4x_theme_core/Cargo.toml"
    )
    val rustSources = rootProject.file(
        "rust/m4x_theme_core/src"
    )
    val jniOutput = project.file("src/main/jniLibs")

    inputs.file(rustManifest)
    inputs.dir(rustSources)
    outputs.dir(jniOutput)

    // cargo-ndk chạy `cargo metadata` trước khi chuyển tiếp lệnh build.
    // Vì vậy phải đứng ngay trong thư mục chứa Cargo.toml.
    workingDir(rustManifest.parentFile)
    commandLine(
        "cargo",
        "ndk",
        "-t", "arm64-v8a",
        "-t", "armeabi-v7a",
        "-t", "x86_64",
        "-P", "24",
        "-o", jniOutput.absolutePath,
        "build",
        "--release"
    )
}

tasks.named("preBuild").configure {
    dependsOn(buildRustThemeValidator)
}

