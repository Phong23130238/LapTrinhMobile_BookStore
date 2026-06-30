import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.project.bookstoreapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.project.bookstoreapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Đọc SERVER_IP từ local.properties (mỗi thành viên tự khai báo, không commit lên git)
        val localProps = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localProps.load(localPropsFile.inputStream())
        }
        val serverIp = localProps.getProperty("SERVER_IP", "10.0.2.2") // Mặc định: emulator
        buildConfigField("String", "SERVER_IP", "\"$serverIp\"")
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // AndroidX & UI Core Libraries
    implementation(libs.appcompat)
    implementation(libs.firebase.database)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.cardview:cardview:1.0.0")

    // Networking - Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Image loading & Utils
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Firebase (Đã gộp chung về phiên bản BoM mới nhất 34.15.0 để đồng bộ)
    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-firestore")
    implementation(libs.firebase.database)

    // --- THƯ VIỆN GIẢI QUYẾT LỖI CANNOT RESOLVE CỦA BẠN ---
    implementation("com.google.firebase:firebase-database")

    // Testing Libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

// Tự động copy dữ liệu JSON vào thư mục assets trước khi build dự án
tasks.register<Copy>("copyFirebaseData") {
    from("../db_firebase.json")
    into("src/main/assets")
}

tasks.named("preBuild") {
    dependsOn("copyFirebaseData")
}
