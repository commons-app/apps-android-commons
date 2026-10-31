---
type: testing-guide
title: Build and test workflow
description: Documents the app's Gradle variants, the unit-test and APK-build tasks used by CI, how instrumented tests are configured, and the limits of existing test evidence.
tags: [testing, gradle, ci, android]
sources:
  - id: openwiki-source-4e3e538728489e7a1208c697
    resource: repo://.github/workflows/android.yml
  - id: openwiki-source-3bfcb28142050978edf94754
    resource: repo://app/build.gradle.kts
  - id: openwiki-source-85c8645f0c3096356baf8b1c
    resource: repo://app/src/androidTest/java/fr/free/nrw/commons/LoginActivityTest.kt
  - id: openwiki-source-990cd2fa25d993209bc2b92e
    resource: repo://app/src/androidTest/java/fr/free/nrw/commons/UploadTest.kt
  - id: openwiki-source-a72fa10cafd93aec97e70c1f
    resource: repo://app/src/test/kotlin/fr/free/nrw/commons/db/MigrationTest.kt
  - id: openwiki-source-0cc61a5b9aac2ce8d008e973
    resource: repo://app/src/test/kotlin/fr/free/nrw/commons/upload/UploadPresenterTest.kt
generated: { by: "copilot", at: "2026-10-31T14:46:52.854Z" }
verified:
  - by: openwiki/0.7.2
    at: 2026-10-31T14:59:24.742Z
---

# Build and test workflow

## Project and variants

This is a single Android application module, `:app`, built with Gradle and Kotlin. `app/build.gradle.kts` defines `prod` and `beta` product flavors and debug/release build types. Unit tests are Kotlin under `app/src/test/kotlin`; Android instrumented tests are under `app/src/androidTest/java` and use `AndroidJUnitRunner`. Unit tests include Android resources and can use Robolectric for Android APIs.

The Android CI workflow sets up JDK 17. On pull requests it populates `local.properties` with test account values from repository secrets and runs:

```sh
./gradlew -Pcoverage testBetaDebugUnitTestCoverage --stacktrace
```

It also builds both debug APK variants:

```sh
bash ./gradlew assembleBetaDebug --stacktrace
bash ./gradlew assembleProdDebug --stacktrace
```

The workflow contains a `connectedBetaDebugAndroidTest` emulator step, but that step is commented out. Instrumented tests therefore exist in the source tree but are not run by the checked-in Android CI workflow at present. To run them locally with a connected emulator or device, use:

```sh
./gradlew connectedBetaDebugAndroidTest
```

## Treat test results as evidence, not the specification

Test maintenance is uneven. Several upload presenter tests are annotated `@Ignore`, and some upload instrumentation tests are also explicitly disabled. Existing tests may encode assumptions from an older implementation or exercise only narrow behavior. Resolve discrepancies by tracing the current production path first, then use an active, focused test as supporting evidence; do not infer intended behavior from an ignored test.

Examples of narrower test surfaces include `MigrationTest`, which checks representative data after legacy-to-Room migration, and `LoginActivityTest`, which checks forgot-password navigation, signup navigation, and a generic orientation change. The login UI test does not assert credential retention, login-button enablement, or server-side authentication. The repository has additional tests for upload, browsing, preferences, and API clients, but their existence alone does not establish that CI executes them or that they cover the current behavior.

## Where to start

- For schema changes, read [Networking and local storage](../architecture/networking-and-storage.md) and run the focused migration test in the beta unit-test task.
- For login, upload, or discovery changes, follow the current source path and the relevant [account flow](../workflows/account-and-authentication.md), [upload lifecycle](../workflows/media-upload.md), or [discovery flow](../workflows/discovery-and-contributions.md) before relying on nearby tests.

## Source-grounded claims

Claims below describe checked-in Gradle and workflow configuration. CI command references distinguish active steps from commented-out steps.
