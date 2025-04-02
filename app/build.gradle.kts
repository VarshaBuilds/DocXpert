plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.docxpert"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.docxpert"
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

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*.kotlin_module",
                "META-INF/versions/9/previous-compilation-data.bin"
            )
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // PDF libraries
    implementation("com.itextpdf:itextg:5.5.10")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    
    // Apache POI dependencies for Word document processing
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3") {
        exclude(group = "org.apache.xmlbeans", module = "xmlbeans")
    }
    implementation("org.apache.poi:poi-scratchpad:5.2.3")
    implementation("org.apache.xmlbeans:xmlbeans:5.1.1")
    
    // Support libraries for Apache POI
    implementation("org.apache.commons:commons-compress:1.23.0")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("com.zaxxer:SparseBitSet:1.2")
    
    // ML Kit for OCR
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0")
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.0")
    implementation("com.google.mlkit:text-recognition-japanese:16.0.0")
    implementation("com.google.mlkit:text-recognition-korean:16.0.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}