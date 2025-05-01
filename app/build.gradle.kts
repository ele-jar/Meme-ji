import java.io.File
import com.android.build.api.variant.ApplicationAndroidComponentsExtension

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
        versionName = "1.0" // Keep this defined here

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("SIGNING_STORE_FILE")
            val storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            val keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            val keyPassword = System.getenv("SIGNING_KEY_PASSWORD")

            if (storeFilePath != null && File(storeFilePath).exists()) {
                this.storeFile = File(storeFilePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            } else {
                 println("!!! Release signing config environment variables not set or keystore file not found. CI should provide secrets.")
                 // Fallback for local builds if needed, though CI should handle signing
                 // For local release builds without env vars, you might need a debug fallback or local properties.
                 // This setup primarily targets CI.
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
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
             isMinifyEnabled = false
        }
    }

    // --- Corrected Variant API block for static naming ---
    val components: ApplicationAndroidComponentsExtension by extensions
    components.onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.all { output -> // output type is com.android.build.api.variant.VariantOutput
            // Get the Property<String> for the output file name
            val outputFileNameProp = output.outputFileName
            // Set the property to the desired static name
            outputFileNameProp.set("Meme-ji-v1.0.apk") // Hardcode the name here
        }
    }
    // --- End of Corrected Variant API block ---


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
