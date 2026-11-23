import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.tesisapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tesisapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "ODOO_DB_NAME", "\"${resolveDbName()}\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8069/\"")
            enableUnitTestCoverage = true // F4: ./gradlew createDebugUnitTestCoverageReport (JaCoCo)
        }
        release {
            isMinifyEnabled = false
            val releaseBaseUrl = resolveReleaseApiBaseUrl()
            validateReleaseApiBaseUrl(releaseBaseUrl)
            buildConfigField("String", "API_BASE_URL", "\"$releaseBaseUrl\"")
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
    kotlinOptions {
        jvmTarget = "11"

    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1") // F4: pruebas de la cola
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Retrofit & Gson
    implementation(libs.retrofit.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Implementación de OkHttp usando el catálogo
    implementation(platform(libs.okhttp.bom)) // Sincroniza versiones
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor) // Soluciona error 'logging'
    implementation(libs.okhttp.urlconnection)     // Soluciona error 'JavaNetCookieJar'

    implementation(libs.osmdroid.android)
    implementation(libs.androidx.preference)

    implementation(libs.play.services.location)
    implementation("io.coil-kt:coil:2.4.0")
    implementation("io.coil-kt:coil-compose:2.4.0")
    // Coil 2.x ya usa OkHttp; "coil-network-okhttp" es un artefacto de Coil 3 (io.coil-kt.coil3) y no existe en 2.4.0

    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    // F4: cola sin conexión (WorkManager con Hilt) y fotos de tareas (orientación EXIF)
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
}

fun resolveDbName(): String {
    return (
        project.findProperty("ODOO_DB_NAME") as? String
            ?: System.getenv("ODOO_DB_NAME")
            ?: "odoo"
        ).trim()
}

fun resolveReleaseApiBaseUrl(): String {
    return (
        project.findProperty("ODOO_API_BASE_URL") as? String
            ?: System.getenv("ODOO_API_BASE_URL")
            ?: "https://example.invalid/"
        ).trim()
}

fun validateReleaseApiBaseUrl(value: String) {
    val uri = try {
        URI(value)
    } catch (_: Exception) {
        throw GradleException("ODOO_API_BASE_URL is not a valid URL: $value")
    }
    val host = uri.host?.lowercase().orEmpty()
    val scheme = uri.scheme?.lowercase().orEmpty()
    if (scheme != "https") {
        throw GradleException("ODOO_API_BASE_URL must use https for release builds.")
    }
    if (host.isBlank()) {
        throw GradleException("ODOO_API_BASE_URL must include a valid host.")
    }
    if (host == "localhost" || host == "127.0.0.1" || host == "0.0.0.0" || host == "10.0.2.2") {
        throw GradleException("ODOO_API_BASE_URL cannot point to loopback hosts in release builds.")
    }
    if (!value.endsWith("/")) {
        throw GradleException("ODOO_API_BASE_URL must end with '/'.")
    }
}