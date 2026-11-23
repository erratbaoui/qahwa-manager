plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.hilt.android)
  alias(libs.plugins.ksp)
  alias(libs.plugins.room)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example.qahwamanager"
  compileSdk = 37
  defaultConfig {
    applicationId = "com.qahwa.manager"
    minSdk = 24
    targetSdk = 37
    // CI passes -PversionCode=<run number> and -PversionName=<tag>, so every release can
    // install over the one before it.
    versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1
    versionName = (findProperty("versionName") as String?) ?: "1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // The permanent release key. CI decodes it from a GitHub secret; nobody else has it.
  // Without QAHWA_KEYSTORE_FILE the release build is left unsigned.
  val keystoreFile = providers.environmentVariable("QAHWA_KEYSTORE_FILE").orNull
  if (keystoreFile != null) {
    signingConfigs.create("release") {
      storeFile = file(keystoreFile)
      storePassword = providers.environmentVariable("QAHWA_KEYSTORE_PASSWORD").get()
      keyAlias = providers.environmentVariable("QAHWA_KEY_ALIAS").get()
      keyPassword = providers.environmentVariable("QAHWA_KEY_PASSWORD").get()
    }
  }

  buildTypes {
    release {
      signingConfig = signingConfigs.findByName("release")
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = false
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }

  // Exported Room schemas, needed by MigrationTestHelper.
  sourceSets {
    getByName("androidTest").assets.directories.add("$projectDir/schemas")
  }
}

kotlin {
  jvmToolchain(17)
}

room {
  schemaDirectory("$projectDir/schemas")
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Hilt
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  implementation(libs.hilt.lifecycle.viewmodel.compose)

  // Room
  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  ksp(libs.room.compiler)
  androidTestImplementation(libs.room.testing)
  androidTestImplementation(libs.kotlinx.coroutines.test)

  // Kotlinx Serialization & Coroutines
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.play.services)

  // Firebase (Auth + Cloud Firestore) & Google Code Scanner
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  implementation(libs.play.services.code.scanner)
}
