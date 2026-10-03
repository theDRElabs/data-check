import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Release signing credentials are read from the environment (CI secrets), or for
// local builds from an untracked keystore/keystore.properties.
//
// A release key was committed to this repository and became public on 2026-10-03.
// It was purged from history but is burned. Generate a new one; never commit it.
// See AGENTS.md and the Release signing section of README.md.
val keystorePropsFile = rootProject.file("keystore/keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}

fun secret(name: String): String? =
    providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
        ?: keystoreProps.getProperty(name)?.takeIf { it.isNotBlank() }

val releaseStoreFile = secret("KEYSTORE_STORE_FILE")?.let { rootProject.file(it) }
    ?: keystorePropsFile.parentFile?.resolve("datacheck-release.p12")
val releaseStorePassword = secret("KEYSTORE_STORE_PASSWORD")
val releaseKeyAlias = secret("KEYSTORE_KEY_ALIAS")
val releaseStoreType = secret("KEYSTORE_STORE_TYPE") ?: "PKCS12"

val canSignRelease = releaseStoreFile != null && releaseStoreFile.exists() &&
    releaseStorePassword != null && releaseKeyAlias != null

if (!canSignRelease) {
    logger.lifecycle(
        "No release keystore available: assembleRelease will produce an UNSIGNED APK. " +
            "Set KEYSTORE_STORE_FILE, KEYSTORE_STORE_PASSWORD and KEYSTORE_KEY_ALIAS, " +
            "or provide an untracked keystore/keystore.properties."
    )
}

android {
    namespace = "com.drelabs.datacheck"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.drelabs.datacheck"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = releaseStoreFile
                storeType = releaseStoreType
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = secret("KEYSTORE_KEY_PASSWORD") ?: releaseStorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (canSignRelease) {
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
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.work:work-runtime-ktx:2.9.1")

    testImplementation("junit:junit:4.13.2")
}
