plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.docexpert"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.docexpert"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

        // PDF Processing (iText)
        implementation(libs.itext7.core)

        // File Picker (for selecting PDFs)
        implementation(libs.filepicker)

        // Image Loading (for thumbnails)
        implementation(libs.glide)
        annotationProcessor(libs.compiler)

        // Material Design UI Components
        implementation(libs.material.v170)
    }

