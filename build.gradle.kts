// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.github.triplet.play) apply false
    alias(libs.plugins.getkeepsafe.dexcount)
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.hilt) apply false
}

subprojects{
    tasks.withType<Test>().configureEach {
        jvmArgs = (jvmArgs ?: emptyList()) + listOf("--add-opens=java.base/java.lang=ALL-UNNAMED")
    }
}