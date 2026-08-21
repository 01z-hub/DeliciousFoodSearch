import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties =
    Properties().apply {
        val propertiesFile = rootProject.file("local.properties")
        if (propertiesFile.exists()) {
            propertiesFile.inputStream().use(::load)
        }
    }

val amapApiKey =
    localProperties.getProperty("AMAP_API_KEY")
        ?: providers.environmentVariable("AMAP_API_KEY").orNull.orEmpty()

android {
    namespace = "com.zyb.deliciousfoodsearch"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.zyb.deliciousfoodsearch"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        manifestPlaceholders["AMAP_API_KEY"] = amapApiKey

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.compose.ui:ui:1.11.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.11.3")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("com.amap.api:search:9.7.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.11.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
