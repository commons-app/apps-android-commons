---
type: data-architecture
title: Networking and local storage
description: Describes the distinct HTTP client paths for MediaWiki services, JSON APIs, and images, plus Room, legacy SQLite migration, cookies, and preference-backed state.
tags: [networking, storage, room, mediawiki, wikidata]
sources:
  - id: openwiki-source-22a66d808a78dbebadf03f93
    resource: repo://app/src/main/java/fr/free/nrw/commons/contributions/ContributionDao.kt
  - id: openwiki-source-77377549d68595ad3e297d5e
    resource: repo://app/src/main/java/fr/free/nrw/commons/crypto/CryptoUtils.kt
  - id: openwiki-source-f478693d758e0577d97e1820
    resource: repo://app/src/main/java/fr/free/nrw/commons/db/AppDatabase.kt
  - id: openwiki-source-2221150639eac6824815089a
    resource: repo://app/src/main/java/fr/free/nrw/commons/di/CommonsApplicationModule.kt
  - id: openwiki-source-1a5d674969c836be34792218
    resource: repo://app/src/main/java/fr/free/nrw/commons/di/NetworkingModule.kt
  - id: openwiki-source-3230406f58cf99a865a58529
    resource: repo://app/src/main/java/fr/free/nrw/commons/media/CustomOkHttpNetworkFetcher.kt
  - id: openwiki-source-fc109e3f436dfcacd3c7faab
    resource: repo://app/src/main/java/fr/free/nrw/commons/OkHttpConnectionFactory.kt
  - id: openwiki-source-758f836820d30cbbd3917087
    resource: repo://app/src/main/java/fr/free/nrw/commons/wikidata/CommonsServiceFactory.kt
  - id: openwiki-source-9e02cfb851078a9532d60a6a
    resource: repo://app/src/main/java/fr/free/nrw/commons/wikidata/cookies/CommonsCookieStorage.kt
generated: { by: "copilot", at: "2026-10-31T14:59:24.742Z" }
verified:
  - by: openwiki/0.7.2
    at: 2026-10-31T14:59:24.742Z
---

# Networking and local storage

## HTTP client paths

`NetworkingModule` provides a DI-managed OkHttp client with 120-second connection, read, and write timeouts, a shared User-Agent interceptor, logging, and an HTTP cache. `OkHttpJsonApiClient` and `CustomOkHttpNetworkFetcher` consume this injected client for JSON API calls and image retrieval.

Retrofit MediaWiki and Wikidata interfaces use a separate path: the module creates `CommonsServiceFactory` from `OkHttpConnectionFactory.getClient(cookieJar)`. That client attaches `CommonsCookieJar`, adds the common User-Agent header, logs basic request details with authorization and cookie headers redacted, and turns unsuccessful HTTP or MediaWiki error responses into failures. `CommonsServiceFactory` caches Retrofit instances by base URL. When changing transport behavior, check both client paths rather than assuming a single shared OkHttp configuration.

The `prod` and `beta` product flavors define their own Commons API, Commons web, image, campaign, and Wikidata endpoint values in `app/build.gradle.kts`. Retrofit service providers use those `BuildConfig` URLs to create endpoint-specific services.

Cookies are stored through `CommonsCookieStorage` in the default preference store. On load, the stored value is passed through `CryptoUtils.decrypt`, an AES-GCM helper backed by an Android Keystore key. Decryption failure returns the input unchanged, so legacy plaintext is parsed as-is and then re-saved (the re-save happens only when decrypt returned the input unchanged). On save, the JSON is encrypted, but if encryption fails the plaintext JSON is stored instead, so encryption is best-effort rather than guaranteed. Logout clears the cookie jar.

## Local state and migrations

`AppDatabase` is the Room database (`commons_room.db`), currently schema version 22. Its entities and DAOs cover queued/completed contributions, depictions, upload status, reviews, nearby places, bookmarks, categories, and recent search/language data. `CommonsApplicationModule` registers the known Room migrations and also enables destructive fallback for migration paths without a registered migration.

The app also retains `DBOpenHelper` for the older `commons.db` schema. The Room 21-to-22 migration creates the corresponding Room tables and copies existing categories, bookmarks, bookmark items, recent searches, and recent languages from the legacy database when it exists. Check both the Room schema and legacy helper when modifying older tables or migration behavior.

Preferences are provided as a shared `JsonKvStore` over the app's preference store. The app uses that store for simple settings and serialized values; account credentials are managed separately through Android's `AccountManager` by `SessionManager`.

Contribution records persist upload state through `ContributionDao`: saves and updates refresh `dateModified`, and initial saves set `dateUploadStarted`. The upload workflow depends on these durable rows to resume work after the activity closes; see [Media upload lifecycle](../workflows/media-upload.md).

## Tests and extension points

`MigrationTest` exercises migration from a legacy database into Room and asserts representative data is retained. It is useful evidence for the migration path, but verify the current migration and schema directly when changing persistence. Focused upload and discovery behavior is covered in the linked workflow pages.

Network providers and database registration live in the Dagger modules. Add service interfaces through `CommonsServiceFactory` and `NetworkingModule`; add Room entities/DAOs to `AppDatabase` and register every migration in the shared application module.

## Source-grounded claims

Claims below describe the current source, not an assumption that all app requests share one client.
