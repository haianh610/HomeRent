import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    packagingOptions {
        resources {
            excludes += setOf(
                "META-INF/*.kotlin_module",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt"
            )
        }
    }
    namespace = "com.example.homerent"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.homerent"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Đọc API key từ local.properties
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            // Dùng use để đảm bảo stream được đóng
            FileInputStream(localPropertiesFile).use { fis ->
                properties.load(fis)
            }
        } else {
            // Tùy chọn: Ghi log cảnh báo nếu file không tồn tại
            logger.warn("local.properties file not found. MAPS_API_KEY might be missing.")
        }

        manifestPlaceholders["MAPS_API_KEY"] = properties.getProperty("MAPS_API_KEY", "") // Lấy key MAPS_API_KEY, trả về "" nếu không tìm thấy
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
        viewBinding =true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation ("com.google.android.gms:play-services-maps:19.1.0")
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui) // Check for latest version
    // implementation 'com.google.android.gms:play-services-location:21.2.0'

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation ("com.github.bumptech.glide:glide:4.12.0") // Kiểm tra phiên bản mới nhất
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation ("de.hdodenhof:circleimageview:3.1.0") // Check for the latest version
}