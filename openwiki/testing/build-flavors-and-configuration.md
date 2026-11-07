---
type: build-configuration
title: Build flavors and configuration
description: Reference for the Android app's prod and beta flavor matrix, build-type behavior, generated configuration, signing gates, test credentials, artifact naming, and configuration precedence. Use it when changing an endpoint, variant, release build, or flavor-sensitive test.
tags: [gradle, build-flavors, buildconfig, release, testing]
verified:
  - by: openwiki/0.7.2
    at: 2026-10-31T15:07:16.361Z
sources:
  - id: openwiki-source-3bfcb28142050978edf94754
    resource: repo://app/build.gradle.kts
  - id: openwiki-source-fffa434b6e0d22b824f14883
    resource: repo://app/src/androidTest/java/fr/free/nrw/commons/UITestHelper.kt
  - id: openwiki-source-1a5d674969c836be34792218
    resource: repo://app/src/main/java/fr/free/nrw/commons/di/NetworkingModule.kt
  - id: openwiki-source-2970fd06903651ec519ae37a
    resource: repo://app/src/main/java/fr/free/nrw/commons/utils/ConfigUtils.kt
  - id: openwiki-source-3b0e70723597a7d28bf1a3ac
    resource: repo://app/src/main/java/fr/free/nrw/commons/wikidata/WikidataEditService.kt
generated: { by: "openwiki/0.7.2", at: "2026-10-31T15:07:16.361Z" }
---

# Build flavors and configuration

The `:app` module defines one flavor dimension, `tier`, with `prod` and `beta` flavors, combined with `debug` and `release` build types. The source of truth is `app/build.gradle.kts`; dependency versions are centralized in `gradle/libs.versions.toml`. For task selection and CI gates, see [Build, test, and quality workflow](build-and-quality.md).

## Flavor matrix

The flavors select the Wikimedia site and package identity at build time. Endpoint ownership and the API boundaries behind these constants are documented in [MediaWiki and Wikidata integrations](../integrations/mediawiki-and-wikidata.md).

| Concern | `prod` | `beta` |
|---|---|---|
| Application id | `fr.free.nrw.commons` | `fr.free.nrw.commons.beta` |
| Commons API | `https://commons.wikimedia.org/w/api.php` | `https://commons.wikimedia.beta.wmflabs.org/w/api.php` |
| Commons web/image base | `https://commons.wikimedia.org` and `https://upload.wikimedia.org/wikipedia/commons` | `https://commons.wikimedia.beta.wmflabs.org` and `https://upload.beta.wmflabs.org/wikipedia/commons` |
| Wikidata API/site | `https://www.wikidata.org/w/api.php` / `https://www.wikidata.org` | Same Wikidata endpoints as prod |
| Campaign feed | `campaigns.json` | `campaigns_beta_active.json` |
| Account type | `fr.free.nrw.commons` | `fr.free.nrw.commons.beta` |
| Wikidata depicts / creator properties | `P180` / `P170` | `P245962` / `P253075` |
| Content-provider authority prefix | `fr.free.nrw.commons...` | `fr.free.nrw.commons.beta...` |

Both flavors define the same `BuildConfig` field set: API and web URLs, signup/password-reset/privacy URLs, file-usages URL, campaign URL, account type, all contribution/modification/category/recent-search/recent-language/bookmark authorities, `COMMIT_SHA`, test credential fields, and the depicts/creator property IDs. Keep the field sets symmetrical: a field added to only one flavor makes the other variant fail at compilation. The flavor-specific authority and account-type prefixes allow prod and beta to be installed side by side without authenticator or content-provider collisions (`app/build.gradle.kts#L124-L213`, `app/build.gradle.kts#L215-L303`).

At runtime, `ConfigUtils.isBetaFlavour` is derived from `BuildConfig.FLAVOR == "beta"`; application, networking, upload, and Wikidata code uses that signal where behavior cannot be expressed as a URL alone. For example, beta Wikidata editing substitutes the beta-site depicts item behavior, while the networking dependency graph passes the flavor-selected campaigns URL into `OkHttpJsonApiClient` (`app/src/main/java/fr/free/nrw/commons/utils/ConfigUtils.kt#L10-L23`, `app/src/main/java/fr/free/nrw/commons/di/NetworkingModule.kt#L85-L100`, `app/src/main/java/fr/free/nrw/commons/wikidata/WikidataEditService.kt#L52-L92`).

## Build-type matrix

| Build type | Shrinking | Version name | Coverage | Signing |
|---|---|---|---|---|
| `debug` | Disabled; the ProGuard configuration is still attached to the variant | Appends `-debug-<current git branch>` | Enables unit-test and Android-test coverage | Android debug signing |
| `release` | Enabled with the default Android rules plus `app/proguard-rules.txt` | Base `versionName` | Not enabled by this build-type block | Debug key by default; CI release signing only under the gated condition below |

The shared defaults are `compileSdk`/`targetSdk` 36, `minSdk` 21, `versionCode` 1067, `versionName` `6.8.0`, multidex, Java/Kotlin target 17, BuildConfig generation, view binding, and Compose. Archive naming starts with `app-commons-v<versionName>-<branch>`; the branch comes from `git rev-parse --abbrev-ref HEAD`, and a failed git lookup yields `null` rather than stopping configuration (`app/build.gradle.kts#L48-L68`, `app/build.gradle.kts#L93-L112`, `app/build.gradle.kts#L305-L316`, `app/build.gradle.kts#L562-L584`).

Release shrinking uses `app/proguard-rules.txt`. The rules preserve reflection and serialization contracts used by Retrofit/Gson and selected application model, auth, Wikidata, file-usage, logging, ACRA, RecyclerView, and `Parcelable` classes; do not remove a keep rule without checking the corresponding reflective or serialized path (`app/proguard-rules.txt#L25-L43`, `app/proguard-rules.txt#L52-L76`, `app/proguard-rules.txt#L88-L100`).

## Signing and publishing gate

The build computes one gate, `CI == "true"` **and** the repository-root `play.p12` file exists. Only when both conditions hold does it:

1. apply the `com.github.triplet.play` publishing plugin;
2. populate the `release` signing config from environment variables (`keystore_password`, `key_alias`, and `key_password`) and `../nr-commons.keystore`; and
3. configure the publisher for the `alpha` track using the service-account environment variable and `../play.p12`.

Otherwise, `release` explicitly uses the Android debug signing config. The ordinary workflow assembles debug APKs, so this release signing path is separate from the normal CI build gate. Keep keystore paths, passwords, aliases, and service-account values in the CI secret/environment mechanism; never put their values in this page or source control. The legacy `sign.sh` script contains a machine-specific jarsigner command and is not the Gradle release configuration (`app/build.gradle.kts#L42-L46`, `app/build.gradle.kts#L81-L103`, `app/build.gradle.kts#L547-L559`, `sign.sh#L1-L3`). For operational release diagnostics, see [Release and diagnostics](../operations/release-and-diagnostics.md).

## Test credentials and configuration sources

Test credentials are not defaults in the build script. During Gradle configuration, both flavors read `TEST_USER_NAME` and `TEST_USER_PASSWORD` from the root `local.properties` file and embed the resulting values into `BuildConfig.TEST_USERNAME` and `BuildConfig.TEST_PASSWORD`. The file is opened unconditionally, so a build requires a readable `local.properties`; missing properties become null-like generated values rather than usable credentials. Keep this file local or provision it from CI secrets, and do not copy credential values into documentation (`app/build.gradle.kts#L208-L212`, `app/build.gradle.kts#L298-L300`, `app/build.gradle.kts#L533-L545`).

The connected-test helper rejects an empty or `"null"` generated credential with a configuration error before attempting login. This makes missing test configuration distinct from a remote authentication failure (`app/src/androidTest/java/fr/free/nrw/commons/UITestHelper.kt#L162-L178`). The checked-in test workflow describes the same contract: CI writes equivalent properties from repository secrets, while local login-dependent tests require the developer to supply them (`openwiki/testing/build-and-quality.md#L15-L20`). Do not log, commit, or reproduce the credential values here.

Other configuration has narrower ownership and does not override flavor constants:

| Source | Consumed by | Effect and precedence |
|---|---|---|
| `defaultConfig` in `app/build.gradle.kts` | Android Gradle Plugin | Shared SDK, version, application defaults, and archive base name |
| `productFlavors` | Android Gradle Plugin / generated `BuildConfig` | Overrides the shared application id and supplies the selected endpoint/property/authority values |
| `buildTypes` | Android Gradle Plugin | Adds debug suffix/coverage or release shrinking/signing behavior to the selected flavor |
| `local.properties` | `getTestUserName()` / `getTestPassword()` | Supplies only embedded test credentials; it does not select a flavor or endpoint |
| CI environment and files | signing/publishing gate | Supplies release signing and Play publishing only when `CI=true` and `play.p12` exists |
| Git metadata | `COMMIT_SHA`, archive name, debug suffix | Stamps the short commit and current branch; unavailable metadata becomes `null` |
| `gradle.properties` | Gradle/Android tooling | Enables AndroidX/Jetifier, caching, R8 full-mode setting, non-transitive R classes, non-final resource IDs, JVM memory, and KAPT K2; it does not replace flavor values (`gradle.properties#L15-L29`) |

When a configuration change is flavor-sensitive, assemble both `assembleBetaDebug` and `assembleProdDebug`, and run the relevant flavor lint/tests as described in [Build, test, and quality workflow](build-and-quality.md#recommended-narrow-validation-by-subsystem).

## Static analysis and connected-test setup

Ktlint reads `app/ktlint-legacy-files.txt` and excludes listed legacy paths. Detekt builds on its default configuration with `app/detekt.yml` and `app/detekt-baseline.xml`. Android lint does not abort the build on findings and explicitly disables `MissingTranslation` and `ExtraTranslation`; these settings affect quality interpretation, not the flavor matrix (`app/build.gradle.kts#L16-L40`, `app/build.gradle.kts#L342-L345`).

The connected `connectedBetaDebugAndroidTest` and `connectedProdDebugAndroidTest` tasks depend on `disableAnimations`, which writes zero window, transition, and animator scales through `adb`. The test options also disable animations and pass `clearPackageData=true` to the instrumentation runner. These hooks stabilize device tests but do not cause the checked-in CI workflow to run instrumentation; that workflow currently leaves its emulator step commented out (`app/build.gradle.kts#L59-L61`, `app/build.gradle.kts#L332-L340`, `app/build.gradle.kts#L514-L531`).
