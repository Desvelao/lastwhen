import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// App parameters: -P gradle property > environment variable > app.properties > neutral default.
val appFile = Properties().apply {
    rootProject.file("app.properties").takeIf { it.exists() }?.reader()?.use { load(it) }
}
fun appParam(key: String, default: String): String =
    (findProperty(key) as String?)
        ?: System.getenv(key)?.takeIf { it.isNotBlank() }
        ?: appFile.getProperty(key)
        ?: default

// Release signing: environment variables > keystore.properties (git-ignored) > unsigned.
val signingProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.reader()?.use { load(it) }
}
fun signing(envName: String, propName: String): String? =
    System.getenv(envName)?.takeIf { it.isNotBlank() } ?: signingProps.getProperty(propName)
val signingStoreFile = signing("SIGNING_KEYSTORE", "storeFile")

android {
    namespace = "app.lasttime"
    compileSdk = 37

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        applicationId = appParam("APP_ID", "io.github.desvelao.lastwhen")
        minSdk = 26
        targetSdk = 37
        versionCode = appParam("APP_VERSION_CODE", "1").toInt()
        versionName = appParam("APP_VERSION_NAME", "0.1.0")
        resValue("string", "app_name", appParam("APP_NAME", "LastWhen"))
        // Project link (for an About screen). Blank hides it.
        val appUrl = appParam("APP_URL", "https://github.com/Desvelao/lastwhen").replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "APP_URL", "\"$appUrl\"")
    }

    signingConfigs {
        if (signingStoreFile != null) {
            create("release") {
                storeFile = rootProject.file(signingStoreFile)
                storePassword = signing("SIGNING_STORE_PASSWORD", "storePassword")
                keyAlias = signing("SIGNING_KEY_ALIAS", "keyAlias")
                keyPassword = signing("SIGNING_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
        resValues = true
        buildConfig = true
    }

    // Generates locales_config.xml from the values-xx folders (per-app language on Android 13+).
    androidResources {
        generateLocaleConfig = true
    }

    // An in-app language switcher needs every language in the bundle, not split per device language.
    bundle {
        language {
            enableSplit = false
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true // Robolectric needs the merged resources
        unitTests.all {
            // In the Docker container the user has no passwd entry, so user.home is "?"; Robolectric stores its downloads there.
            System.getenv("HOME")?.let { home -> it.systemProperty("user.home", home) }
        }
    }

    // The exported Room schemas feed MigrationTestHelper in instrumented tests.
    sourceSets.getByName("androidTest").assets.directories.add("$projectDir/schemas")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

// A release build without signing details would silently produce an unsigned artifact; refuse instead.
gradle.taskGraph.whenReady {
    val releaseRequested = allTasks.any { it.path == ":app:assembleRelease" || it.path == ":app:bundleRelease" }
    if (releaseRequested && signingStoreFile == null) {
        throw GradleException(
            "Release signing is not configured: set SIGNING_KEYSTORE (+ SIGNING_STORE_PASSWORD, " +
                "SIGNING_KEY_ALIAS, SIGNING_KEY_PASSWORD) or create keystore.properties.",
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime)
    implementation(libs.glance.appwidget)
    implementation(libs.biometric)
    implementation(libs.appcompat)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.room.testing)
    // room-testing (androidTest) needs serialization >= 1.8, but consistent resolution pins the debug classpath at 1.7.3.
    debugImplementation(libs.serialization.core)
    testImplementation(libs.org.json) // the android.jar copy is only a stub on the JVM
    testImplementation(libs.coroutines.test)
}
