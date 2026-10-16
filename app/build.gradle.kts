plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Every CI run gets a higher version, so a new APK installs over the old one.
val buildNumber = providers.environmentVariable("GITHUB_RUN_NUMBER").orNull?.toIntOrNull() ?: 1

// The app's own signing key, given to CI as secrets (see README › Building). Without it the debug key is used.
val keystorePath = providers.environmentVariable("HOLOVOICE_KEYSTORE").orNull?.takeIf { file(it).isFile }
val keystorePassword = providers.environmentVariable("HOLOVOICE_KEYSTORE_PASSWORD").orNull?.takeIf { it.isNotEmpty() }
if (keystorePath != null && keystorePassword == null) {
    throw GradleException("HOLOVOICE_KEYSTORE is set but HOLOVOICE_KEYSTORE_PASSWORD is missing")
}

android {
    namespace = "com.ttggg5.holovoice"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ttggg5.holovoice"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"
    }

    signingConfigs {
        if (keystorePath != null && keystorePassword != null) {
            create("holovoice") {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = "holovoice"
                keyPassword = keystorePassword
            }
        }
    }
    val appSigning = signingConfigs.findByName("holovoice") ?: signingConfigs.getByName("debug")

    buildTypes {
        debug {
            // Same key as release, so either APK installs over the other.
            signingConfig = appSigning
        }
        release {
            // Optimized (R8) build for smooth scrolling.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = appSigning
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
    lint {
        // Print every finding into the build log so CI failures are readable without the report artifact.
        textReport = true
        textOutput = file("stdout")
    }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.documentfile)
    implementation(libs.coil.compose)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.reorderable)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    // The real org.json: the one in android.jar is only stubs in unit tests.
    testImplementation(libs.org.json)
}
