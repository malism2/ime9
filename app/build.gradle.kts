import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val malismPropsFile = rootProject.file("malism.properties")
val malismProps = Properties().apply {
    if (malismPropsFile.exists()) {
        malismPropsFile.inputStream().use(::load)
    }
}

android {
    namespace = "com.malism.ime9"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.malism.ime9"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            check(malismPropsFile.exists()) {
                "Missing ${malismPropsFile.name}; required for release signing"
            }
            storeFile = file(malismProps.getProperty("storeFile"))
            storePassword = malismProps.getProperty("storePassword")
            keyAlias = malismProps.getProperty("keyAlias")
            keyPassword = malismProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
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

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("test") {
            resources.srcDir("src/main/assets")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    testImplementation("junit:junit:4.13.2")
}
