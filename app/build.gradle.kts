import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}
android {

    namespace = "atikyan.silva.plantcare"
    compileSdk = 36

    defaultConfig {
        applicationId = "atikyan.silva.plantcare"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) localProps.load(localFile.inputStream())

        buildConfigField("String", "GEMINI_KEY_ADVICE",    "\"" + (localProps["GEMINI_KEY_ADVICE"]   ?: "") + "\"")
        buildConfigField("String", "GEMINI_KEY_DETECT",    "\"" + (localProps["GEMINI_KEY_DETECT"]   ?: "") + "\"")
        buildConfigField("String", "GEMINI_KEY_DIAGNOSE",  "\"" + (localProps["GEMINI_KEY_DIAGNOSE"] ?: "") + "\"")
        buildConfigField("String", "GEMINI_KEY_SEARCH",    "\"" + (localProps["GEMINI_KEY_SEARCH"]   ?: "") + "\"")
        buildConfigField("String", "GEMINI_KEY_BOTANIST",  "\"" + (localProps["GEMINI_KEY_BOTANIST"] ?: "") + "\"")
        buildConfigField("String", "GEMINI_KEY_ADVICE2",   "\"" + (localProps["GEMINI_KEY_ADVICE2"]  ?: "") + "\"")
        buildConfigField("String", "GEMINI_KEY_DETECT2",   "\"" + (localProps["GEMINI_KEY_DETECT2"]  ?: "") + "\"")
        buildConfigField("String", "GEMINI_KEY_DIAGNOSE2", "\"" + (localProps["GEMINI_KEY_DIAGNOSE2"]?: "") + "\"")
        buildConfigField("String", "GEMINI_KEY_SEARCH2",   "\"" + (localProps["GEMINI_KEY_SEARCH2"]  ?: "") + "\"")
        buildConfigField("String", "GEMINI_KEY_BOTANIST2", "\"" + (localProps["GEMINI_KEY_BOTANIST2"]?: "") + "\"")
        buildConfigField("String", "GEMINI_KEY_1",    "\"" + (localProps["GEMINI_KEY_1"]   ?: "") + "\"")
        buildConfigField("String", "GEMINI_KEY_2",    "\"" + (localProps["GEMINI_KEY_2"]   ?: "") + "\"")
        buildConfigField("String", "GEMINI_KEY_3",  "\"" + (localProps["GEMINI_KEY_3"] ?: "") + "\"")
        buildConfigField("String", "GEMINI_KEY_4",    "\"" + (localProps["GEMINI_KEY_4"]   ?: "") + "\"")
        buildConfigField("String", "GEMINI_KEY_5",  "\"" + (localProps["GEMINI_KEY_5"] ?: "") + "\"")
        buildConfigField("String", "GEMINI_KEY_6",   "\"" + (localProps["GEMINI_KEY_6"]  ?: "") + "\"")
        buildConfigField("String", "GEMINI_KEY_7",   "\"" + (localProps["GEMINI_KEY_7"]  ?: "") + "\"")
        buildConfigField("String", "GEMINI_KEY_8", "\"" + (localProps["GEMINI_KEY_8"]?: "") + "\"")
    }

    buildFeatures {
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.airbnb.android:lottie:6.1.0")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.google.guava:guava:33.0.0-android")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation("com.google.mlkit:image-labeling:17.0.7")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.github.bumptech.glide:glide:4.16.0")
}