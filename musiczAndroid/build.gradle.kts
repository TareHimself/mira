plugins {
    id("com.android.application")
    kotlin("android")

    id("io.realm.kotlin")
    id("org.jetbrains.compose")

    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "com.tarehimself.mangaz.android"
    compileSdk = 33
    defaultConfig {
        applicationId = "com.tarehimself.mangaz.android"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.7"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

val decomposeVersion = "2.0.0-compose-experimental"
dependencies {
    implementation(project(":shared"))
    implementation("androidx.compose.ui:ui:1.4.3")
    implementation("androidx.compose.ui:ui-tooling:1.4.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.4.3")
    implementation("androidx.compose.foundation:foundation:1.4.3")
    implementation("androidx.compose.material:material:1.4.3")
    implementation("androidx.activity:activity-compose:1.7.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // Ktor
    implementation("io.ktor:ktor-client-android:2.3.2")

    implementation("com.arkivanov.decompose:decompose:$decomposeVersion")
    implementation("com.arkivanov.decompose:extensions-compose-jetbrains:$decomposeVersion")


}

