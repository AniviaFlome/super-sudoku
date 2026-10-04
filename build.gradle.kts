// Root build file: all shared versions live in gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.agp) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
