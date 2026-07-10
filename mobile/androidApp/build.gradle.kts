plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

android {
    namespace = "com.seatwise.mobile.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.seatwise.mobile"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // 演示签名：keystore 由 CI 用 keytool 生成（不入库）；本地没有 keystore 时回退到 debug 签名。
    // “随便啥签名”即可——目的是产出可直接安装到手机的已签名 APK。
    val releaseKeystore = rootProject.file("seatwise-release.jks")
    signingConfigs {
        create("release") {
            if (releaseKeystore.exists()) {
                storeFile = releaseKeystore
                storePassword = System.getenv("KS_PASS") ?: "seatwise123"
                keyAlias = System.getenv("KS_ALIAS") ?: "seatwise"
                keyPassword = System.getenv("KS_KEYPASS") ?: "seatwise123"
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = if (releaseKeystore.exists())
                signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(compose.runtime)
    implementation(compose.ui)
    implementation(compose.foundation)
    implementation(compose.material3)
}
