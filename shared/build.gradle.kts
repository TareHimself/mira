plugins {
    //kotlin("android")
    kotlin("multiplatform")
    id("com.android.library")

    id("io.realm.kotlin")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")

    id("kotlin-parcelize")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    targetHierarchy.default()

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    
    listOf(
        iosX64(),
        iosArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        val ktorVersion = "2.3.2"
        val decomposeVersion = "2.0.0-compose-experimental"
        val okioVersion = "3.5.0"

        val commonMain by getting {
            dependencies {
                // Compose
                implementation(compose.runtime)
                implementation(compose.foundation)
                // Material 3
                implementation(compose.material3)
                implementation(compose.material)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)

                // Icon Packs https://github.com/DevSrSouza/compose-icons
                implementation("br.com.devsrsouza.compose.icons:font-awesome:1.1.0")
                implementation("br.com.devsrsouza.compose.icons:octicons:1.1.0")
                implementation("br.com.devsrsouza.compose.icons:eva-icons:1.1.0")
                implementation("br.com.devsrsouza.compose.icons:css-gg:1.1.0")

                // UUID https://github.com/benasher44/uuid
                implementation("com.benasher44:uuid:0.7.1")

                // Ktor
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

                implementation("media.kamel:kamel-image:0.6.0")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1") // Add to use coroutines with the SDK
                implementation("io.realm.kotlin:library-base:1.8.0") // Add to only use the local database

                // Decompose
                implementation("com.arkivanov.decompose:decompose:$decomposeVersion")
                implementation("com.arkivanov.decompose:extensions-compose-jetbrains:$decomposeVersion")


                // Date Time https://github.com/Kotlin/kotlinx-datetime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

                // Koin
                implementation("io.insert-koin:koin-core:3.4.2")
                implementation("io.insert-koin:koin-compose:1.0.3")

                // Logging https://github.com/AAkira/Napier
                implementation("io.github.aakira:napier:2.6.1")

                // Coroutine worker https://github.com/Autodesk/coroutineworker
                implementation("com.autodesk:coroutineworker:0.8.3")


                // hashing https://github.com/goncalossilva/kotlinx-murmurhash
                implementation("com.goncalossilva:murmurhash:0.4.0")


                // https://square.github.io/okio/multiplatform/
                implementation("com.squareup.okio:okio:$okioVersion")

                // https://github.com/russhwolf/multiplatform-settings
                implementation("com.russhwolf:multiplatform-settings-no-arg:1.0.0")
            }
        }

//        val jsMain by getting {
//            dependencies {
//                implementation("com.squareup.okio:okio-nodefilesystem:$okioVersion")
//            }
//        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("com.squareup.okio:okio-fakefilesystem:$okioVersion")

            }
        }

        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-android:$ktorVersion")
                implementation("org.objenesis:objenesis:3.2")
                implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
            }
        }
//        val iosMain by creating {
//            // ...
//            dependencies {
//                implementation("io.ktor:ktor-client-darwin:$ktorVersion")
//            }
//        }


    }
}

android {
    namespace = "com.tarehimself.mira"
    compileSdk = 33
    defaultConfig {
        minSdk = 24
    }
    sourceSets {
        getByName("main") {
            manifest.srcFile ("src/androidMain/AndroidManifest.xml")
        }
    }
}
dependencies {
    implementation("com.google.android.material:material:1.9.0")
}
