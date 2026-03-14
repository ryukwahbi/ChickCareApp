import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.ksp)
}

// Load local.properties file
val localProperties = Properties()
// Try app-level local.properties first, then root-level
val localPropertiesFile = file("local.properties").takeIf { it.exists() } 
    ?: rootProject.file("local.properties").takeIf { it.exists() }
if (localPropertiesFile != null) {
    localProperties.load(FileInputStream(localPropertiesFile))
    println("✓ Loaded local.properties from: ${localPropertiesFile.absolutePath}")
    val cloudName = localProperties.getProperty("CLOUDINARY_CLOUD_NAME", "")
    val apiKey = localProperties.getProperty("CLOUDINARY_API_KEY", "")
    val apiSecret = localProperties.getProperty("CLOUDINARY_API_SECRET", "")
    if (cloudName.isNotEmpty() && apiKey.isNotEmpty() && apiSecret.isNotEmpty()) {
        println("✓ Cloudinary credentials found in local.properties")
    } else {
        println("⚠️ WARNING: Cloudinary credentials are missing or empty in local.properties!")
    }
} else {
    println("⚠️ WARNING: local.properties file not found!")
}

android {
    namespace = "com.bisu.chickcare"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bisu.chickcare"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Load Cloudinary credentials from local.properties (secure - not in source code)
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${localProperties.getProperty("CLOUDINARY_CLOUD_NAME", "")}\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"${localProperties.getProperty("CLOUDINARY_API_KEY", "")}\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", "\"${localProperties.getProperty("CLOUDINARY_API_SECRET", "")}\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
            excludes.add("META-INF/services/javax.annotation.processing.Processor")
        }
        resources {
            excludes.add("META-INF/services/javax.annotation.processing.Processor")
            // Suppress warnings about companion objects in Google Play Services
            // This is a known issue with desugaring that doesn't affect functionality
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
    androidResources {
        noCompress.add("tflite")
    }
    @Suppress("DEPRECATION")
    kotlinOptions {
        @Suppress("DEPRECATION")
        freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
}

configurations.all {
    exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    resolutionStrategy {
        force("org.tensorflow:tensorflow-lite-support-api:0.4.4")
    }
}

dependencies {

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.foundation)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.ucrop)
    implementation(libs.androidx.appcompat)
    implementation(libs.play.services.base)
    implementation(libs.play.services.location)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.guava)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.tensorflow.lite) {
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }
    implementation(libs.tensorflow.lite.gpu) {
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }
    implementation(libs.tensorflow.lite.task.vision) {
        exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }
    implementation(libs.tensorflow.lite.support) {
        exclude(group = "org.tensorflow", module = "tensorflow-lite-support-api")
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }
    implementation(libs.tensorflow.lite.gpu.delegate.plugin) {
        exclude(group = "org.tensorflow", module = "tensorflow-lite")
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }
    // ML Kit - Face detection for invalid human selfie guard
    implementation(libs.face.detection)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
