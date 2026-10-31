---
type: build-configuration
title: Build flavors and configuration
description: Describes the app module's prod and beta product flavors, the BuildConfig fields and content-provider authorities that differ between them, release and debug build types, static analysis, and CI-only signing.
tags: [gradle, build-flavors, buildconfig, release, ktlint, detekt]
verified:
  - by: openwiki/0.7.2
    at: 2026-10-31T14:59:24.742Z
sources:
  - id: openwiki-source-3bfcb28142050978edf94754
    resource: repo://app/build.gradle.kts
generated: { by: "copilot", at: "2026-10-31T14:59:24.742Z" }
---

# Build flavors and configuration

All build logic for the single `:app` module lives in `app/build.gradle.kts`, with dependency versions in `gradle/libs.versions.toml`. For how tests and CI use these variants see [Build and test workflow](build-and-quality.md).

## Two flavors, one `tier` dimension

The `tier` flavor dimension has `prod` and `beta` flavors. They differ almost entirely through `buildConfigField` values that the runtime reads instead of hard-coding hosts:

| Concern | prod | beta |
|---|---|---|
| Application id | `fr.free.nrw.commons` | `fr.free.nrw.commons.beta` |
| MediaWiki API host | `commons.wikimedia.org` | `commons.wikimedia.beta.wmflabs.org` |
| Account type | `fr.free.nrw.commons` | `fr.free.nrw.commons.beta` |
| Depicts / creator property | `P180` / `P170` | `P245962` / `P253075` |
| Campaigns feed | production campaigns JSON | beta campaigns JSON |

Each flavor also defines image, home, mobile, password-reset and sign-up URLs, plus a `*_AUTHORITY` string for every content provider (contributions, modifications, categories, recent searches, recent languages, bookmarks and bookmark locations/items). Because the authorities and account type are prefixed per flavor, both flavors can be installed side by side without provider or authenticator collisions. Both flavors additionally embed `COMMIT_SHA` (short git hash) and the `TEST_USERNAME`/`TEST_PASSWORD` fields read from `local.properties`. Note that `getTestUserName` opens `local.properties` unconditionally, so that file must exist for the build to configure.

Code consults the flavor at runtime through helpers such as `ConfigUtils.isBetaFlavour` (for example, the Wikidata edit service substitutes a beta-site depicts item). When adding a new site-specific URL, add the field to both flavors; a field present in only one will fail to compile for the other.

## Build types, versions and signing

`defaultConfig` sets `compileSdk`/`targetSdk` 36, `minSdk` 21, enables multidex, and builds archive names from the version name and current git branch. Release builds enable minification with `proguard-rules.txt`, and sign with the debug key by default; the real `release` signing config is populated from environment variables only when `CI=true` and `play.p12` exists, which is also the condition for applying the Play publishing plugin. Without that key file, release builds are signed with the debug key. Debug builds disable minification, append `-debug-<branch>` to the version name, and enable unit and Android-test coverage.

Java and Kotlin target 17; `buildConfig`, view binding and Compose are enabled. `resolutionStrategy` force-pins `androidx.annotation`, `timber` and `fragment` to older versions, so upgrading those via the version catalog alone has no effect until the forced pins are removed.

## Static analysis

ktlint is configured with a legacy-file exclusion list (`ktlint-legacy-files.txt`) so files not yet reformatted are skipped, and detekt uses `detekt.yml` with a baseline (`detekt-baseline.xml`) plus Compose rules. New code is expected to pass both; entries should be removed from the legacy list and baseline as files are fixed. Lint does not fail the build (`abortOnError = false`).

## Instrumented tests

The `connectedBetaDebugAndroidTest` and `connectedProdDebugAndroidTest` tasks depend on a `disableAnimations` task so emulator animations do not destabilize UI tests; the CI workflow's emulator step is currently commented out.
