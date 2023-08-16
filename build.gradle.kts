plugins {
    //trick: for the same plugin versions in all sub-modules
    //kotlin("android").version("1.8.20").apply(false)
    kotlin("multiplatform").apply(false)

    kotlin("plugin.serialization") version "1.8.20"


    id("com.android.application").apply(false)
    id("com.android.library").apply(false)
    id("io.realm.kotlin").version("1.8.0").apply(false)
    id("org.jetbrains.compose").version("1.4.3").apply(false)

}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
