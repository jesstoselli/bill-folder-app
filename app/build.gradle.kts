import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// ----------------------------------------------------------------------------
// Override do API_BASE_URL via local.properties (gitignored).
//
// Permite alternar entre emulador / device + backend local na LAN /
// device + backend remoto sem editar arquivo versionado. Adicionar no
// local.properties (mesmo arquivo onde mora o sdk.dir):
//
//   api.base.url=http://192.168.2.123:5077/v1/   # device + Mac local na LAN
//   api.base.url=https://api.billfolder.app/v1/  # device + remoto (debug)
//
// Sem a property, cada buildType cai no default conservador:
//   debug   → http://10.0.2.2:5077/v1/   (alias do emulador pro localhost)
//   release → https://api.billfolder.app/v1/
// ----------------------------------------------------------------------------
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val apiBaseUrlOverride: String? = localProps.getProperty("api.base.url")?.takeIf { it.isNotBlank() }

android {
    namespace = "com.billfolder.android"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.billfolder.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Default: alias do emulador Android pro localhost da host machine.
            // Em device físico OU pra apontar pro remoto, sobrescreve via
            // `api.base.url=...` em local.properties (ver comentário no topo
            // do arquivo). Sem o override, vai pro emulador.
            val debugUrl = apiBaseUrlOverride ?: "http://10.0.2.2:5077/v1/"
            buildConfigField("String", "API_BASE_URL", "\"$debugUrl\"")
            isMinifyEnabled = false
        }
        release {
            // Default: produção. Override raramente é útil aqui (caso de
            // querer testar uma release build minificada contra staging).
            val releaseUrl = apiBaseUrlOverride ?: "https://api.billfolder.app/v1/"
            buildConfigField("String", "API_BASE_URL", "\"$releaseUrl\"")
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
        compose = true
        buildConfig = true
    }

    // ------------------------------------------------------------------------
    // Android Lint — checks oficiais do AGP (a11y, Compose, Kotlin, perf, etc)
    // ------------------------------------------------------------------------
    lint {
        // Quebra a build no primeiro erro. Avisos não bloqueiam.
        abortOnError = true
        // Reporta avisos como erros — bom em CI, doloroso em dev. Mantemos
        // false aqui; o pipeline pode rodar com `-Pandroid.lint.warningsAsErrors=true`.
        warningsAsErrors = false
        // Falha quando o lint não consegue rodar uma checagem (ex: faltando dep).
        absolutePaths = false
        // Continua executando outros checks após erros não-fatais (mais info por run).
        ignoreWarnings = false
        // Mostra todos os erros, mesmo passando do limite.
        checkAllWarnings = true
        // Inclui módulos de teste na varredura.
        checkTestSources = false
        ignoreTestSources = true
        // Habilita checks específicos de Compose (Modifier reuse, etc).
        checkDependencies = true
        // Saída HTML pra leitura humana (gerada em build/reports/lint-results-*.html).
        htmlReport = true
        xmlReport = true
        textReport = false
    }
}

dependencies {
    // Compose (BOM gerencia as versões dos -ui, -material3, etc)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.google.fonts)

    // AndroidX core / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Persistência
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
