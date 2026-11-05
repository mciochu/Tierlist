buildscript {
    dependencies {
        // AGP 9 includes Kotlin support. Pin the matching KGP version for Compose.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    id("com.android.application") version "9.3.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
