---
type: repository-guide
title: Repository quickstart
description: Routes contributors to the Android app's runtime, networking and persistence, authentication, upload and discovery workflows, and validation guidance.
tags: [quickstart, android, navigation, development]
verified:
  - by: openwiki/0.7.2
    at: 2026-10-31T14:35:25.887Z
sources:
  - id: openwiki-source-4e3e538728489e7a1208c697
    resource: repo://.github/workflows/android.yml
  - id: openwiki-source-186e96b8d6739f3745947903
    resource: repo://app/src/main/AndroidManifest.xml
  - id: openwiki-source-23775c3de52f3ab95a13cb8b
    resource: repo://README.md
  - id: openwiki-source-e620d7484b72a53c7fa812cd
    resource: repo://settings.gradle.kts
generated: { by: "copilot", at: "2026-10-31T14:35:25.887Z" }
---

# Repository quickstart

This repository builds the Wikimedia Commons Android app. The Gradle settings currently include one application module, `:app`; the Android manifest names `CommonsApplication` and declares `LoginActivity` as the launcher. `UploadActivity` also accepts external image and audio share intents.

## Find the right guide

| If you are changing... | Start here |
|---|---|
| Application startup, Dagger injection, or activity navigation | [Application runtime and dependency graph](architecture/app-runtime.md) |
| MediaWiki/Wikidata API calls, HTTP clients, Room, or migrations | [Networking and local storage](architecture/networking-and-storage.md) |
| Login, account state, challenge codes, or CSRF tokens | [Account and authentication flow](workflows/account-and-authentication.md) |
| Selecting, describing, queuing, or uploading media | [Media upload lifecycle](workflows/media-upload.md) |
| Explore lists, contribution history, categories, or Nearby places | [Discovery and contribution browsing](workflows/discovery-and-contributions.md) |
| Gradle variants, CI, or deciding which tests to trust | [Build and test workflow](testing/build-and-quality.md) |

Use the manifest for Android component entrypoints, the Dagger component and builder modules for injectable Android components, and the feature flow page for the state owner and data source behind a screen. When tests disagree with production code, verify the current source path: several repository tests are ignored or stale, and the CI workflow does not currently run its instrumented-test step.

## Source-grounded claims

The app's purpose and runtime entrypoints below are grounded in the repository README and Android configuration.
