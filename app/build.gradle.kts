import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization")
}

android {
    namespace = "dev.supersudoku.app"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "dev.supersudoku.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release key location: local.properties (developer machine) or
    // environment (CI). Never commit key material (*.jks/*.keystore are
    // gitignored). Without a key the release build stays unsigned but still
    // compiles for verification; sign before uploading to Play.
    val keyProps = Properties()
    val keyPropsFile = rootProject.file("local.properties")
    if (keyPropsFile.exists()) {
        keyPropsFile.inputStream().use { keyProps.load(it) }
    }
    fun keyProp(name: String, env: String): String? =
        keyProps.getProperty(name) ?: System.getenv(env)

    // Fail fast on a half-configured key: an incomplete signingConfig either
    // breaks assembleRelease obscurely or ships an unsigned "release" build.
    val releaseKey = mapOf(
        "storeFile" to keyProp("release.storeFile", "KEYSTORE_PATH"),
        "storePassword" to keyProp("release.storePassword", "KEYSTORE_PASSWORD"),
        "keyAlias" to keyProp("release.keyAlias", "KEY_ALIAS"),
        "keyPassword" to keyProp("release.keyPassword", "KEY_PASSWORD"),
    )
    val presentKeys = releaseKey.filterValues { !it.isNullOrBlank() }.keys
    if (presentKeys.isNotEmpty() && presentKeys.size < releaseKey.size) {
        error(
            "Incomplete release signing config: set all of " +
                "${releaseKey.keys} (local.properties or KEYSTORE_PATH/KEYSTORE_PASSWORD/" +
                "KEY_ALIAS/KEY_PASSWORD env), or none for an unsigned verification build. " +
                "Missing: ${(releaseKey.keys - presentKeys).joinToString()}"
        )
    }

    signingConfigs {
        create("release") {
            releaseKey["storeFile"]?.let { storeFile = rootProject.file(it) }
            storePassword = releaseKey["storePassword"]
            keyAlias = releaseKey["keyAlias"]
            keyPassword = releaseKey["keyPassword"]
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (presentKeys.size == releaseKey.size) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        animationsDisabled = true
    }
}

// androidx.test 1.6.x needs tracing 1.1.0, but the Compose BOM pins 1.0.0
// (strict constraint). Force the newer tracing: tiny stable lib, safe bump.
configurations.configureEach {
    resolutionStrategy.force("androidx.tracing:tracing:1.1.0")
}

dependencies {
    implementation(project(":core"))
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.datastore.preferences)
    implementation(libs.serialization.json)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.ui:ui-tooling")
}
