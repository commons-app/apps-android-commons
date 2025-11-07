import java.util.Properties
import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.parcelize)
}

apply(from = "$rootDir/jacoco.gradle")

val isRunningOnTravisAndIsNotPRBuild = System.getenv("CI") == "true" && file("../play.p12").exists()

if (isRunningOnTravisAndIsNotPRBuild) {
    apply(plugin = "com.github.triplet.play")
}

android {
    namespace = "fr.free.nrw.commons"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.free.nrw.commons"
        minSdk = 21
        targetSdk = 35
        versionCode = 1059
        versionName = "6.1.0"

        setProperty("archivesBaseName", "app-commons-v$versionName-" + getBranchName())
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        multiDexEnabled = true

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    sourceSets {
        getByName("test") {
            // Use kotlin only in tests (for now)
            java.srcDirs("src/test/kotlin")

            // Use main assets and resources in test
            assets.srcDirs("src/main/assets")
            resources.srcDirs("src/main/resources")
        }
    }

    signingConfigs {
        create("release") {
            // Configure keystore based on env vars in Travis for automated alpha builds
            if(isRunningOnTravisAndIsNotPRBuild) {
                storeFile = file("../nr-commons.keystore")
                storePassword = System.getenv("keystore_password")
                keyAlias = System.getenv("key_alias")
                keyPassword = System.getenv("key_password")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.txt")
            testProguardFile("test-proguard-rules.txt")

            signingConfig = signingConfigs.getByName("debug")
            if (isRunningOnTravisAndIsNotPRBuild) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.txt")
            testProguardFile("test-proguard-rules.txt")

            versionNameSuffix = "-debug-" + getBranchName()
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    configurations.all {
        resolutionStrategy {
            force("androidx.annotation:annotation:1.1.0")
            force("com.jakewharton.timber:timber:4.7.1")
            force("androidx.fragment:fragment:1.3.6")
        }
        exclude(module = "okhttp-ws")
    }

    flavorDimensions += "tier"
    productFlavors {
        create("prod") {
            dimension = "tier"
            applicationId = "fr.free.nrw.commons"

            buildConfigField("String", "WIKIMEDIA_API_POTD", "\"https://commons.wikimedia.org/w/api.php?action=featuredfeed&feed=potd&feedformat=rss&language=en\"")
            buildConfigField("String", "WIKIMEDIA_API_HOST", "\"https://commons.wikimedia.org/w/api.php\"")
            buildConfigField("String", "WIKIDATA_API_HOST", "\"https://www.wikidata.org/w/api.php\"")
            buildConfigField("String", "WIKIDATA_URL", "\"https://www.wikidata.org\"")
            buildConfigField("String", "WIKIMEDIA_FORGE_API_HOST", "\"https://tools.wmflabs.org/\"")
            buildConfigField("String", "WIKIMEDIA_CAMPAIGNS_URL", "\"https://raw.githubusercontent.com/commons-app/campaigns/master/campaigns.json\"")
            buildConfigField("String", "IMAGE_URL_BASE", "\"https://upload.wikimedia.org/wikipedia/commons\"")
            buildConfigField("String", "HOME_URL", "\"https://commons.wikimedia.org/wiki/\"")
            buildConfigField("String", "COMMONS_URL", "\"https://commons.wikimedia.org\"")
            buildConfigField("String", "WIKIDATA_URL", "\"https://www.wikidata.org\"")
            buildConfigField("String", "MOBILE_HOME_URL", "\"https://commons.m.wikimedia.org/wiki/\"")
            buildConfigField("String", "MOBILE_META_URL", "\"https://meta.m.wikimedia.org/wiki/\"")
            buildConfigField("String", "SIGNUP_LANDING_URL", "\"https://commons.m.wikimedia.org/w/index.php?title=Special:CreateAccount&returnto=Main+Page&returntoquery=welcome%3Dyes\"")
            buildConfigField("String", "SIGNUP_SUCCESS_REDIRECTION_URL", "\"https://commons.m.wikimedia.org/w/index.php?title=Main_Page&welcome=yes\"")
            buildConfigField("String", "FORGOT_PASSWORD_URL", "\"https://commons.wikimedia.org/wiki/Special:PasswordReset\"")
            buildConfigField("String", "PRIVACY_POLICY_URL", "\"https://github.com/commons-app/commons-app-documentation/blob/master/android/Privacy-policy.md\"")
            buildConfigField("String", "FILE_USAGES_BASE_URL", "\"https://commons.wikimedia.org/w/api.php?action=query&format=json&formatversion=2\"")
            buildConfigField("String", "ACCOUNT_TYPE", "\"fr.free.nrw.commons\"")
            buildConfigField("String", "CONTRIBUTION_AUTHORITY", "\"fr.free.nrw.commons.contributions.contentprovider\"")
            buildConfigField("String", "MODIFICATION_AUTHORITY", "\"fr.free.nrw.commons.modifications.contentprovider\"")
            buildConfigField("String", "CATEGORY_AUTHORITY", "\"fr.free.nrw.commons.categories.contentprovider\"")
            buildConfigField("String", "RECENT_SEARCH_AUTHORITY", "\"fr.free.nrw.commons.explore.recentsearches.contentprovider\"")
            buildConfigField("String", "RECENT_LANGUAGE_AUTHORITY", "\"fr.free.nrw.commons.recentlanguages.contentprovider\"")
            buildConfigField("String", "BOOKMARK_AUTHORITY", "\"fr.free.nrw.commons.bookmarks.contentprovider\"")
            buildConfigField("String", "BOOKMARK_LOCATIONS_AUTHORITY", "\"fr.free.nrw.commons.bookmarks.locations.contentprovider\"")
            buildConfigField("String", "BOOKMARK_ITEMS_AUTHORITY", "\"fr.free.nrw.commons.bookmarks.items.contentprovider\"")
            buildConfigField("String", "COMMIT_SHA", "\"" + getBuildVersion().toString() + "\"")
            buildConfigField("String", "TEST_USERNAME", "\"" + getTestUserName() + "\"")
            buildConfigField("String", "TEST_PASSWORD", "\"" + getTestPassword() + "\"")
            buildConfigField("String", "DEPICTS_PROPERTY", "\"P180\"")
            buildConfigField("String", "CREATOR_PROPERTY", "\"P170\"")
        }

        create("beta") {
            dimension = "tier"
            applicationId = "fr.free.nrw.commons.beta"

            // What values do we need to hit the BETA versions of the site / api ?
            buildConfigField("String", "WIKIMEDIA_API_POTD", "\"https://commons.wikimedia.org/w/api.php?action=featuredfeed&feed=potd&feedformat=rss&language=en\"")
            buildConfigField("String", "WIKIMEDIA_API_HOST", "\"https://commons.wikimedia.beta.wmflabs.org/w/api.php\"")
            buildConfigField("String", "WIKIDATA_API_HOST", "\"https://www.wikidata.org/w/api.php\"")
            buildConfigField("String", "WIKIDATA_URL", "\"https://www.wikidata.org\"")
            buildConfigField("String", "WIKIMEDIA_FORGE_API_HOST", "\"https://tools.wmflabs.org/\"")
            buildConfigField("String", "WIKIMEDIA_CAMPAIGNS_URL", "\"https://raw.githubusercontent.com/commons-app/campaigns/master/campaigns_beta_active.json\"")
            buildConfigField("String", "IMAGE_URL_BASE", "\"https://upload.beta.wmflabs.org/wikipedia/commons\"")
            buildConfigField("String", "HOME_URL", "\"https://commons.wikimedia.beta.wmflabs.org/wiki/\"")
            buildConfigField("String", "COMMONS_URL", "\"https://commons.wikimedia.beta.wmflabs.org\"")
            buildConfigField("String", "WIKIDATA_URL", "\"https://www.wikidata.org\"")
            buildConfigField("String", "MOBILE_HOME_URL", "\"https://commons.m.wikimedia.beta.wmflabs.org/wiki/\"")
            buildConfigField("String", "MOBILE_META_URL", "\"https://meta.m.wikimedia.beta.wmflabs.org/wiki/\"")
            buildConfigField("String", "SIGNUP_LANDING_URL", "\"https://commons.m.wikimedia.beta.wmflabs.org/w/index.php?title=Special:CreateAccount&returnto=Main+Page&returntoquery=welcome%3Dyes\"")
            buildConfigField("String", "SIGNUP_SUCCESS_REDIRECTION_URL", "\"https://commons.m.wikimedia.beta.wmflabs.org/w/index.php?title=Main_Page&welcome=yes\"")
            buildConfigField("String", "FORGOT_PASSWORD_URL", "\"https://commons.wikimedia.beta.wmflabs.org/wiki/Special:PasswordReset\"")
            buildConfigField("String", "PRIVACY_POLICY_URL", "\"https://github.com/commons-app/commons-app-documentation/blob/master/android/Privacy-policy.md\"")
            buildConfigField("String", "FILE_USAGES_BASE_URL", "\"https://commons.wikimedia.org/w/api.php?action=query&format=json&formatversion=2\"")
            buildConfigField("String", "ACCOUNT_TYPE", "\"fr.free.nrw.commons.beta\"")
            buildConfigField("String", "CONTRIBUTION_AUTHORITY", "\"fr.free.nrw.commons.beta.contributions.contentprovider\"")
            buildConfigField("String", "MODIFICATION_AUTHORITY", "\"fr.free.nrw.commons.beta.modifications.contentprovider\"")
            buildConfigField("String", "CATEGORY_AUTHORITY", "\"fr.free.nrw.commons.beta.categories.contentprovider\"")
            buildConfigField("String", "RECENT_SEARCH_AUTHORITY", "\"fr.free.nrw.commons.beta.explore.recentsearches.contentprovider\"")
            buildConfigField("String", "RECENT_LANGUAGE_AUTHORITY", "\"fr.free.nrw.commons.beta.recentlanguages.contentprovider\"")
            buildConfigField("String", "BOOKMARK_AUTHORITY", "\"fr.free.nrw.commons.beta.bookmarks.contentprovider\"")
            buildConfigField("String", "BOOKMARK_LOCATIONS_AUTHORITY", "\"fr.free.nrw.commons.beta.bookmarks.locations.contentprovider\"")
            buildConfigField("String", "BOOKMARK_ITEMS_AUTHORITY", "\"fr.free.nrw.commons.beta.bookmarks.items.contentprovider\"")
            buildConfigField("String", "COMMIT_SHA", "\"" + getBuildVersion().toString() + "\"")
            buildConfigField("String", "TEST_USERNAME", "\"" + getTestUserName() + "\"")
            buildConfigField("String", "TEST_PASSWORD", "\"" + getTestPassword() + "\"")
            buildConfigField("String", "DEPICTS_PROPERTY", "\"P245962\"")
            buildConfigField("String", "CREATOR_PROPERTY", "\"P253075\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
    buildToolsVersion = buildToolsVersion
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        jniLibs {
            excludes += listOf("META-INF/androidx.*")
        }
        resources {
            excludes += listOf(
                "META-INF/androidx.*",
                "META-INF/proguard/androidx-annotations.pro",
                "/META-INF/LICENSE.md",
                "/META-INF/LICENSE-notice.md"
            )
        }
    }
    testOptions {
        animationsDisabled = true
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
        unitTests.all {
            it.jvmArgs("-noverify")
        }
    }
    lint {
        abortOnError = false
        disable += listOf("MissingTranslation", "ExtraTranslation")
    }
}

dependencies {
    // Utils
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.adapter.rxjava)
    implementation(libs.rxandroid)
    implementation(libs.rxjava)
    implementation(libs.rxbinding)
    implementation(libs.rxbinding.appcompat)
    implementation(libs.facebook.fresco)
    implementation(libs.facebook.fresco.middleware)
    implementation(libs.apache.commons.lang3)

    // UI
    implementation("${libs.viewpagerindicator.library.get()}@aar")
    implementation(libs.photoview)
    implementation(libs.android.sdk)
    implementation(libs.android.plugin.scalebar)

    implementation(libs.timber)
    implementation(libs.android.material)
    implementation(libs.dexter)

    // Jetpack Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.ui.viewbinding)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.foundation.layout)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.adapterdelegates4.kotlin.dsl.viewbinding)
    implementation(libs.adapterdelegates4.pagination)
    implementation(libs.androidx.paging.runtime.ktx)
    testImplementation(libs.androidx.paging.common.ktx)
    implementation(libs.androidx.paging.rxjava2.ktx)
    implementation(libs.androidx.recyclerview)

    // Logging
    implementation(libs.acra.dialog)
    implementation(libs.acra.mail)
    implementation(libs.slf4j.api)
    implementation(libs.logback.android.classic) {
        exclude(group = "com.google.android", module = "android")
    }
    implementation(libs.logging.interceptor)

    // Dependency injector
    implementation(libs.dagger.android)
    implementation(libs.dagger.android.support)
    kapt(libs.dagger.android.processor)
    kapt(libs.dagger.compiler)
    annotationProcessor(libs.dagger.android.processor)

    implementation(libs.kotlin.reflect)

    //Mocking
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)
    testImplementation(libs.powermock.module.junit)
    testImplementation(libs.powermock.api.mockito)
    testImplementation(libs.mockk)

    // Unit testing
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.runner)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.rules)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.livedata.testing.ktx)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.soloader)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.fragment.testing)
    testImplementation(libs.commons.io)

    // Android testing
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(libs.androidx.espresso.contrib)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.annotation)
    androidTestImplementation(libs.mockwebserver)
    androidTestImplementation(libs.androidx.uiautomator)

    // Debugging
    debugImplementation(libs.leakcanary.android)

    // Support libraries
    implementation(libs.androidx.browser)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.exifinterface)
    implementation(libs.recyclerview.fastscroll)

    //swipe_layout
    implementation(libs.swipelayout.library)

    //Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.rxjava)
    kapt(libs.androidx.room.compiler)

    // Preferences
    implementation(libs.androidx.preference)
    implementation(libs.androidx.preference.ktx)

    //Android Media
    implementation(libs.juanitobananas.androidDmediaUtil)
    implementation(libs.androidx.multidex)

    // Kotlin + coroutines
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.coroutines.rx2)
    testImplementation(libs.androidx.work.testing)

    //Glide
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    kaptTest(libs.androidx.databinding.compiler)
    kaptAndroidTest(libs.androidx.databinding.compiler)

    implementation(libs.coordinates2country.android) {
        exclude(group = "com.google.android", module = "android")
    }

    //OSMDroid
    implementation(libs.osmdroid.android)
    constraints {
        implementation(libs.kotlin.stdlib.jdk7) {
            because("kotlin-stdlib-jdk7 is now a part of kotlin-stdlib")
        }
        implementation(libs.kotlin.stdlib.jdk8) {
            because("kotlin-stdlib-jdk8 is now a part of kotlin-stdlib")
        }
    }
}

tasks.register<Exec>("disableAnimations") {
    val adb = "${System.getenv("ANDROID_HOME")}/platform-tools/adb"
    commandLine(adb, "shell", "settings", "put", "global", "window_animation_scale", "0")
    commandLine(adb, "shell", "settings", "put", "global", "transition_animation_scale", "0")
    commandLine(adb, "shell", "settings", "put", "global", "animator_duration_scale", "0")
}

project.gradle.taskGraph.whenReady {
    val connectedBetaDebugAndroidTest = tasks.named("connectedBetaDebugAndroidTest")
    val connectedProdDebugAndroidTest = tasks.named("connectedProdDebugAndroidTest")

    connectedBetaDebugAndroidTest.configure {
        dependsOn("disableAnimations")
    }
    connectedProdDebugAndroidTest.configure {
        dependsOn("disableAnimations")
    }
}

fun getTestUserName(): String? {
    val propFile = rootProject.file("./local.properties")
    val properties = Properties()
    propFile.inputStream().use { properties.load(it) }
    return properties.getProperty("TEST_USER_NAME")
}

fun getTestPassword(): String? {
    val propFile = rootProject.file("./local.properties")
    val properties = Properties()
    propFile.inputStream().use { properties.load(it) }
    return properties.getProperty("TEST_USER_PASSWORD")
}

if (isRunningOnTravisAndIsNotPRBuild) {
    configure<com.github.triplet.gradle.play.PlayPublisherExtension> {
        track = "alpha"
        userFraction = 1.0
        serviceAccountEmail = System.getenv("SERVICE_ACCOUNT_NAME")
        serviceAccountCredentials = file("../play.p12")

        resolutionStrategy = "auto"
        outputProcessor { // this: ApkVariantOutput
            versionNameOverride = "$versionNameOverride.$versionCode"
        }
    }
}

fun getBuildVersion(): String? {
    return try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = stdout
        }
        stdout.toString().trim()
    } catch (e: Exception) {
        null
    }
}

fun getBranchName(): String? {
    return try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
            standardOutput = stdout
        }
        stdout.toString().trim()
    } catch (e: Exception) {
        null
    }
}
