plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.gradle)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.mz.payments.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.mz.payments.sample"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "plugins"
    productFlavors {
        create("base") {
            dimension = "plugins"
            buildConfigField("String", "EXPECTED_PAYMENT_METHOD_IDS", "\"\"")
        }
        create("card") {
            dimension = "plugins"
            buildConfigField("String", "EXPECTED_PAYMENT_METHOD_IDS", "\"card\"")
        }
        create("wallet") {
            dimension = "plugins"
            buildConfigField("String", "EXPECTED_PAYMENT_METHOD_IDS", "\"wallet\"")
        }
        create("all") {
            dimension = "plugins"
            buildConfigField(
                "String",
                "EXPECTED_PAYMENT_METHOD_IDS",
                "\"card,wallet\"",
            )
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = false
        shaders = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

val usePublishedArtifacts = providers.gradleProperty("usePublishedArtifacts")
    .map(String::toBoolean)
    .getOrElse(false)

fun sdkDependency(
    projectPath: String,
    artifactId: String,
): Any = if (usePublishedArtifacts) {
    "io.mz.payments:$artifactId:1.0.0-SNAPSHOT"
} else {
    project(projectPath)
}

dependencies {
    implementation(sdkDependency(":core", "core"))
    add(
        "cardImplementation",
        sdkDependency(":payment-method-card", "payment-method-card"),
    )
    add(
        "walletImplementation",
        sdkDependency(":payment-method-wallet", "payment-method-wallet"),
    )
    add(
        "allImplementation",
        sdkDependency(":payment-method-card", "payment-method-card"),
    )
    add(
        "allImplementation",
        sdkDependency(":payment-method-wallet", "payment-method-wallet"),
    )

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
