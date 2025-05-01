import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.androidx.navigation.safeargs.kotlin)
}

android {
    namespace = "com.example.memesji"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.memesji"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // --- Signing Config Block ---
    // Reads signing information from environment variables (set by GitHub Actions secrets)
    signingConfigs {
        create("release") {
            val storeFile = System.getenv("SIGNING_STORE_FILE") ?: "keystore_placeholder.jks"
            val storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: ""
            val keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: ""
            val keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: ""

            // Only apply signing if the file exists and secrets are provided (i.e., in CI)
            if (storeFile != "keystore_placeholder.jks" && File(storeFile).exists()) {
                storeFile(File(storeFile))
                storePassword(storePassword)
                keyAlias(keyAlias)
                keyPassword(keyPassword)
            } else {
                 println("!!! Release signing config not found or file missing. CI should provide secrets.")
            }
        }
    }
    // --- End of Signing Config Block ---


    buildTypes {
        release {
            // --- Enable Minification for Release ---
            isMinifyEnabled = true // Set to true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // --- Apply the Signing Config ---
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            // Debug builds typically don't need signing config and have minify disabled
             isMinifyEnabled = false
        }
    }
    compileOptions {
        // Keep Java 8 compatibility for wider device support / library compatibility
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = false // Keeping this false as per original
        buildConfig = true
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.swiperefreshlayout)

    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.navigation)
    implementation(libs.bundles.network)

    implementation(libs.glide)
    kapt(libs.glide.compiler)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation("net.lingala.zip4j:zip4j:2.11.5")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
