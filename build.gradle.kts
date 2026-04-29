// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt)
}

// ----------------------------------------------------------------------------
// Detekt — static analysis. Aplicado a todos subprojetos pra escalar quando
// o app virar multi-module. Config global em config/detekt/detekt.yml.
//
// Nota: capturamos `libs.versions.detekt.get()` aqui no escopo do root,
// porque dentro de allprojects {} o accessor `libs` do Version Catalog
// não é visível (escopo de Project, não DefaultProject_Decorated).
// ----------------------------------------------------------------------------
val detektVersion = libs.versions.detekt.get()
val detektConfigFile = files("$rootDir/config/detekt/detekt.yml")

allprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        toolVersion = detektVersion
        config.setFrom(detektConfigFile)
        // Reaplica regras default + nossas overrides (em vez de só nossas).
        buildUponDefaultConfig = true
        // Permite só warnings sem quebrar a build local.
        // CI deve rodar com falha em qualquer issue.
        ignoreFailures = false
        autoCorrect = false
        parallel = true
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        // Ignora código gerado (Hilt/KSP/Room/etc).
        exclude("**/build/**", "**/generated/**")

        reports {
            html.required.set(true)
            xml.required.set(true)
            sarif.required.set(false)
            md.required.set(false)
            txt.required.set(false)
        }
    }
}
