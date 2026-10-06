import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pintodo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pintodo"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "0.4.5"
    }

    // Play 업로드 키: 저장소 밖(~/.android/pintodo/keystore.properties)에 두고 커밋하지 않음
    val keystoreProps = Properties().apply {
        val file = File(System.getenv("PINTODO_KEYSTORE_PROPERTIES") ?: "${System.getProperty("user.home")}/.android/pintodo/keystore.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }
    signingConfigs {
        if (keystoreProps.isNotEmpty()) create("upload") {
            storeFile = file(keystoreProps.getProperty("storeFile"))
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Play 업로드용 (bundleRelease). 키 파일이 없는 PC에서는 서명 없이 만들어짐
            signingConfig = signingConfigs.findByName("upload")
        }
        // 개인 폰에 직접 설치하는 용도 (기존 설치본과 같은 디버그 키) → ./gradlew assembleSideload
        create("sideload") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("com.google.android.play:review:2.0.2")   // 앱 안 별점 요청

    testImplementation("junit:junit:4.13.2")
}
