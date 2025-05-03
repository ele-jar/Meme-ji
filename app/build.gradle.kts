import java.io.File
// Removed ApplicationAndroidComponentsExtension and SingleArtifact imports as they weren't used

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-parcelize")
    // Apply Safe Args BEFORE Kapt
    alias(libs.plugins.androidx.navigation.safeargs.kotlin)
    id("org.jetbrains.kotlin.kapt")
}


android {
    namespace = "com.elejar.memeji" // Make sure this matches your refactored package
    compileSdk = 34

    defaultConfig {
        applicationId = "com.elejar.memeji" // Make sure this matches your refactored package
        minSdk = 24
        targetSdk = 34
        versionCode = 1 // Use the correct versionCode for your release
        versionName = "1.0.0" // Use the correct versionName for your release

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // Use the correct versionName dynamically here
        setProperty("archivesBaseName", "Meme-ji-v${defaultConfig.versionName}")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("SIGNING_STORE_FILE") ?: project.findProperty("SIGNING_STORE_FILE") as? String
            val storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: project.findProperty("SIGNING_STORE_PASSWORD") as? String
            val keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: project.findProperty("SIGNING_KEY_ALIAS") as? String
            val keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: project.findProperty("SIGNING_KEY_PASSWORD") as? String

            // Check if all necessary signing information is present *and* the file exists
            if (storeFilePath != null && !storeFilePath.isNullOrBlank() &&
                storePassword != null && !storePassword.isNullOrBlank() &&
                keyAlias != null && !keyAlias.isNullOrBlank() &&
                keyPassword != null && !keyPassword.isNullOrBlank() &&
                File(storeFilePath).exists()) {
                this.storeFile = File(storeFilePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
                println(">>> Release signing config fully applied from environment/properties.")
            } else {
                // Log specific missing parts if possible (optional, but helpful for debugging)
                if (storeFilePath == null || storeFilePath.isNullOrBlank()) println("!!! Signing Error: SIGNING_STORE_FILE not set or empty.")
                else if (!File(storeFilePath).exists()) println("!!! Signing Error: Keystore file not found at path: $storeFilePath")
                if (storePassword == null || storePassword.isNullOrBlank()) println("!!! Signing Error: SIGNING_STORE_PASSWORD not set or empty.")
                if (keyAlias == null || keyAlias.isNullOrBlank()) println("!!! Signing Error: SIGNING_KEY_ALIAS not set or empty.")
                if (keyPassword == null || keyPassword.isNullOrBlank()) println("!!! Signing Error: SIGNING_KEY_PASSWORD not set or empty.")

                println("!!! Release signing config incomplete. Build might fail or use debug signing.")
                // Prevent assigning null to signingConfig later if essential parts are missing
                // signingConfigs.getByName("release").storeFile = null // Mark as unusable explicitly
            }
        }
    }


    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Apply signing config ONLY if it's fully configured
            if (signingConfigs.getByName("release").storeFile?.exists() == true) {
                 signingConfig = signingConfigs.getByName("release")
                 println(">>> Applying release signing config to release build type.")
            } else {
                 println("!!! WARNING: Not applying signing config to release build type due to missing information.")
                 // Depending on AGP version, build might fail here or use debug signing. Explicitly using debug might be safer if release signing is required.
                 // signingConfig = signingConfigs.getByName("debug") // Uncomment to force debug signing if release fails
            }
        }
        debug {
             isMinifyEnabled = false
             // Ensure debug builds are always debug signed explicitly
             signingConfig = signingConfigs.getByName("debug") // Android Studio usually generates a debug config implicitly
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
        viewBinding = true
        dataBinding = false // Keep explicitly false if not used
        buildConfig = true
    }

    // Optional: Add packaging options if you encounter specific issues later
    // packaging {
    //     resources {
    //         excludes += "/META-INF/{AL2.0,LGPL2.1}"
    //     }
    // }

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
    implementation("net.lingala.zip4j:zip4j:2.11.5") // Make sure this version is desired
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

