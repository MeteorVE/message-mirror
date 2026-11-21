import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Release signing: read from android/key.properties (local builds) or
// RELEASE_* environment variables (CI). Without either, release builds
// fall back to the debug key, which changes on every CI runner and forces
// an uninstall (wiping settings) before each update.
val keystoreProperties = Properties().apply {
    val f = rootProject.file("key.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingValue(propKey: String, envKey: String): String? =
    keystoreProperties.getProperty(propKey) ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("storeFile", "RELEASE_STORE_FILE")

android {
    namespace = "lol.arian.notifmirror"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "lol.arian.notifmirror"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = signingValue("storePassword", "RELEASE_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "RELEASE_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (releaseStoreFile != null) {
                signingConfigs.getByName("release")
            } else {
                logger.warn("Release signing not configured; falling back to the debug key.")
                signingConfigs.getByName("debug")
            }
        }
    }
    applicationVariants.configureEach {
        val appName = "NotifMirror"

        // Use mergedFlavor for compatibility across AGP versions
        val vName = mergedFlavor.versionName ?: defaultConfig.versionName
        val vCode = mergedFlavor.versionCode ?: defaultConfig.versionCode

        outputs.configureEach {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            // ABI filter: arm64-v8a, armeabi-v7a, etc.
            val abiFilter = output.getFilter(com.android.build.OutputFile.ABI) ?: "universal"

            // In Kotlin DSL, assign directly via the internal API
            output.outputFileName = "${appName}-v${vName}(${vCode})-${abiFilter}.apk"
        }
    }

    testOptions {
        unitTests.apply {
            isIncludeAndroidResources = true
        }
    }
}

flutter {
    source = "../.."
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.5.0")
}
