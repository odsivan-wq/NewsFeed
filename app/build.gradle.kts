import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// читаємо ключ з local.properties (цей файл не потрапляє на GitHub)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.example.newsfeed"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.newsfeed"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ключ NewsAPI стає доступним у коді як BuildConfig.NEWS_API_KEY
        buildConfigField("String", "NEWS_API_KEY", "\"${localProps.getProperty("NEWS_API_KEY", "")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // мережа
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    // ViewModel і LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.11.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.11.0")

    // список, оновлення свайпом, картинки
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")
    implementation("com.github.bumptech.glide:glide:5.0.5")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}