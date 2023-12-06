import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.config.JvmTarget

plugins {
    id("com.android.application")
    kotlin("android")

    id("io.realm.kotlin")
    id("org.jetbrains.compose")

    id("org.jetbrains.kotlin.plugin.parcelize")
}


android {
    namespace = "com.tarehimself.mira.android"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.tarehimself.mira.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
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

//        getByName("debug") {
//            // Enables code shrinking, obfuscation, and optimization for only
//            // your project's release build type. Make sure to use a build
//            // variant with `isDebuggable=false`.
//            isMinifyEnabled = true
//
////            // Enables resource shrinking, which is performed by the
////            // Android Gradle plugin.
////            isShrinkResources = true
//
////            // Includes the default ProGuard rules files that are packaged with
////            // the Android Gradle plugin. To learn more, go to the section about
////            // R8 configuration files.
////            proguardFiles(
////                getDefaultProguardFile("proguard-android-optimize.txt"),
////                "proguard-rules.pro"
////            )
//        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8

    }

    kotlin {
        jvmToolchain(17)
    }

//    kotlinOptions {
//        jvmTarget = JvmTarget.JVM_17.toString()
//    }
//
//    java {
//        toolchain {
//            languageVersion.set(JavaLanguageVersion.of(11))
//        }
//    }
//    java {
//        sourceCompatibility = JavaVersion.VERSION_17
//        targetCompatibility = JavaVersion.VERSION_17
//    }

}

//kotlin {
//    jvmToolchain(17)
//}

val decomposeVersion = "2.0.0-compose-experimental"
dependencies {
    implementation(project(":shared"))
    implementation("androidx.compose.ui:ui:1.4.3")
    implementation("androidx.compose.ui:ui-tooling:1.4.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.4.3")
    implementation("androidx.compose.foundation:foundation:1.4.3")

    implementation("androidx.activity:activity-compose:1.7.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    implementation(compose.material3)
    implementation(compose.material)

    // Ktor
    implementation("io.ktor:ktor-client-android:2.3.2")

    implementation("com.arkivanov.decompose:decompose:$decomposeVersion")
    implementation("com.arkivanov.decompose:extensions-compose-jetbrains:$decomposeVersion")

    // koin
    val koin_version = "3.4.2"
    implementation("io.insert-koin:koin-android:$koin_version")

    implementation("org.objenesis:objenesis:3.2")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
//    implementation(files("libs/kmagick-sources.jar"))
}

