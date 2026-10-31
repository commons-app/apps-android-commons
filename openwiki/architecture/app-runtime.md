---
type: application-architecture
title: Application runtime and dependency graph
description: Explains how Android launches Commons, initializes application-wide services and Dagger injection, and routes users into the main activity's signed-in or logged-out navigation.
tags: [android, application, dependency-injection, navigation]
verified:
  - by: openwiki/0.7.2
    at: 2026-10-31T14:59:24.742Z
sources:
  - id: openwiki-source-186e96b8d6739f3745947903
    resource: repo://app/src/main/AndroidManifest.xml
  - id: openwiki-source-7387acd4a61ace0877fd0ffa
    resource: repo://app/src/main/java/fr/free/nrw/commons/CommonsApplication.kt
  - id: openwiki-source-b99f07c7f59c0fcd00129c17
    resource: repo://app/src/main/java/fr/free/nrw/commons/contributions/MainActivity.kt
  - id: openwiki-source-a67e6ed16f49dff4c9e709a2
    resource: repo://app/src/main/java/fr/free/nrw/commons/di/ActivityBuilderModule.kt
  - id: openwiki-source-1943f8eed07a3f4c4278f378
    resource: repo://app/src/main/java/fr/free/nrw/commons/di/CommonsApplicationComponent.kt
generated: { by: "copilot", at: "2026-10-31T14:35:25.887Z" }
---

# Application runtime and dependency graph

## Android entrypoints

The manifest names `CommonsApplication` as the application class. `LoginActivity` is the launcher entrypoint; `UploadActivity` is also exported for image and audio `ACTION_SEND` / `ACTION_SEND_MULTIPLE` intents, while `MainActivity` hosts the primary in-app navigation. The manifest also registers the account authenticator service, upload progress UI, and other feature activities and providers.

`CommonsApplication.onCreate()` initializes ACRA, obtains the singleton application component through `ApplicationlessInjection`, injects itself, and then configures logging, image loading, notifications, language lookup, and RxJava's global error handler. The application owns shared session, database-helper, preference-store, cookie, image-fetcher, and contribution dependencies.

## Dagger graph and Android component injection

`CommonsApplicationComponent` is the singleton Dagger component. It composes the application and networking modules with Android activity, fragment, service, content-provider, upload, contribution, search, depiction, and category modules. `ApplicationlessInjection` builds and caches that component for the process and exposes Android injectors. Activities are registered through `ActivityBuilderModule`; adding an injectable activity requires a corresponding binding there.

## Main navigation and login mode

`LoginActivity` handles authentication before entering the main screen. On `MainActivity` creation, the persisted `login_skipped` preference selects a logged-out pager that opens Explore; otherwise the signed-in navigation is set up, the previous Nearby tab may be restored, and stuck uploads are checked for resumption. Signed-in navigation loads fragments for Contributions, Nearby, Explore, and Bookmarks, while the logged-out pager uses a restricted `NavTabLoggedOut` set. The “More” action opens a different bottom sheet depending on login mode.

`BaseActivity` provides shared theme selection, font-scale adjustment, edge-to-edge setup, and cleanup of activity-scoped Rx disposables. Some activities, including LoginActivity, have their own delegate/lifecycle setup; check the concrete activity before assuming every screen follows the same base class.

## Where to look next

- [Networking and local storage](networking-and-storage.md) describes the separately wired API clients and database ownership.
- [Account and authentication flow](../workflows/account-and-authentication.md) follows login results into persisted account state.
- [Discovery and contribution browsing](../workflows/discovery-and-contributions.md) covers the main navigation destinations.
- [Media upload lifecycle](../workflows/media-upload.md) traces shared upload entrypoints through background execution.

## Source-grounded claims

Claims below describe runtime behavior verified against current source. They do not treat tests as a specification.
