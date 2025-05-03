import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-parcelize")
    // Apply Kapt FIRST
    id("org.jetbrains.kotlin.kapt")
    // Apply Safe Args AFTER Kapt
    alias(libs.plugins.androidx.navigation.safeargs.kotlin)
}


android {
    namespace = "com.elejar.memeji" // Ensure this is correct
    compileSdk = 34

    defaultConfig {
        applicationId = "com.elejar.memeji" // Ensure this is correct
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0" // Use v1.0.0 as requested

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // Dynamic archivesBaseName based on versionName
        setProperty("archivesBaseName", "Meme-ji-v${defaultConfig.versionName}")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("SIGNING_STORE_FILE") ?: project.findProperty("SIGNING_STORE_FILE") as? String
            val storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: project.findProperty("SIGNING_STORE_PASSWORD") as? String
            val keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: project.findProperty("SIGNING_KEY_ALIAS") as? String
            val keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: project.findProperty("SIGNING_KEY_PASSWORD") as? String

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
                println("!!! Release signing config incomplete or keystore file not found at '$storeFilePath'. Build might fail or use debug signing.")
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
            if (signingConfigs.getByName("release").storeFile?.exists() == true) {
                 signingConfig = signingConfigs.getByName("release")
                 println(">>> Applying release signing config to release build type.")
            } else {
                 println("!!! WARNING: Not applying signing config to release build type due to missing information.")
                 // Keep building unsigned if config is missing in CI
            }
        }
        debug {
             isMinifyEnabled = false
             // Ensure debug builds are always debug signed explicitly if necessary
             // signingConfig = signingConfigs.getByName("debug") // Usually handled automatically
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
        dataBinding = false
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
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Apply Kapt plugin configuration if needed
// kapt {
//    correctErrorTypes = true
// }
