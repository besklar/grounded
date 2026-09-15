import com.android.build.api.dsl.ManagedVirtualDevice
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }
val mapsConfigured = localProperties.getProperty("MAPS_API_KEY").orEmpty().isNotBlank()
val mapsApiKey = localProperties.getProperty("MAPS_API_KEY").orEmpty().ifBlank { "DEFAULT_API_KEY" }

android {
    namespace = "com.besklar.grounded"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.besklar.grounded"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        buildConfigField("boolean", "MAPS_CONFIGURED", mapsConfigured.toString())
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"

    testOptions {
        unitTests.apply {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        managedDevices.allDevices {
            create<ManagedVirtualDevice>("pixel2Api31") {
                device = "Pixel 2"
                apiLevel = 31
                systemImageSource = "aosp"
            }
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "ObsoleteSdkInt")
    }
}

kotlin {
    jvmToolchain(17)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.google.hilt.android)
    ksp(libs.google.hilt.compiler)
    implementation(libs.google.maps.compose)
    implementation(libs.google.play.services.location)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockwebserver)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
