// /Users/devtiwari/Desktop/willow-lottery/Src/build.gradle.kts
plugins {
    alias(libs.plugins.android.application) apply false // Use the alias here
    id("com.android.library") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("com.google.gms.google-services") version "4.4.1" apply false
}