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
    
    // Ensure assets are properly included
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
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

    // PDF Processing (iText 7) - Using explicit dependencies instead of the core bundle
    implementation("com.itextpdf:kernel:7.1.15")
    implementation("com.itextpdf:io:7.1.15")
    implementation("com.itextpdf:layout:7.1.15")
    implementation("com.itextpdf:forms:7.1.15")
    implementation("com.itextpdf:pdfa:7.1.15")
    implementation("com.itextpdf:sign:7.1.15")
    implementation("com.itextpdf:barcodes:7.1.15")
    implementation("com.itextpdf:font-asian:7.1.15")
    implementation("com.itextpdf:hyph:7.1.15")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.slf4j:slf4j-simple:1.7.36")

    // File Picker (for selecting PDFs)
    implementation(libs.filepicker)

    // Image Loading (for thumbnails)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    // Material Design UI Components
    implementation(libs.material.v170)

    //used to create word doc
    implementation(libs.apache.poi.ooxml)
    
    // OCR Library - Replace problematic dependency
    // implementation(libs.tesseract.ocr)
    
    // Use Google ML Kit Text Recognition instead
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")
}

