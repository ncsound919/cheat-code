import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.propslab"
    compileSdk = 36
    // The native runtime is built from source so the weights and the code that reads them
    // stay on this device. Only arm64 is shipped: that is the device this app runs on.
    ndkVersion = "26.1.10909125"

    defaultConfig {
        applicationId = "com.propslab"
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=c++_static")
                targets += listOf("propslab_llama")
            }
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    // A release build has to be signed to install, and every future update has to be signed
    // by the same key or Android refuses it. The keystore lives outside version control;
    // keystore.properties holds its password and is ignored with it. A build without those
    // files still works, it just publishes an unsigned APK rather than failing the build.
    val signingProps = Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.isFile) file.inputStream().use { load(it) }
    }
    signingConfigs {
        if (signingProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = rootProject.file(signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.work.runtime.ktx)
    implementation(libs.mlkit.text.recognition)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    compileOnly("org.json:json:20240303")
    testImplementation("org.json:json:20240303")
}
