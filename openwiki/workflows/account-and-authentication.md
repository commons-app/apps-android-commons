---
type: authentication-workflow
title: Account and authentication flow
description: Follows MediaWiki login-token exchange, two-factor and email challenges, account persistence, session flags, cookie-backed API state, and logged-out browsing.
tags: [authentication, account-manager, mediawiki, csrf]
verified:
  - by: openwiki/0.7.2
    at: 2026-10-31T14:35:25.887Z
sources:
  - id: openwiki-source-0550682b2f9d9cae7e79cdfa
    resource: repo://app/src/main/java/fr/free/nrw/commons/auth/csrf/CsrfTokenClient.kt
  - id: openwiki-source-bc29fd999762b8ae486a1bd4
    resource: repo://app/src/main/java/fr/free/nrw/commons/auth/csrf/LogoutClient.kt
  - id: openwiki-source-e956d443ed061c1acde90148
    resource: repo://app/src/main/java/fr/free/nrw/commons/auth/login/LoginClient.kt
  - id: openwiki-source-9265c9f3e93cabe5f2bc4866
    resource: repo://app/src/main/java/fr/free/nrw/commons/auth/LoginActivity.kt
  - id: openwiki-source-e2209d1d44bd9b8447b89f02
    resource: repo://app/src/main/java/fr/free/nrw/commons/auth/SessionManager.kt
  - id: openwiki-source-b99f07c7f59c0fcd00129c17
    resource: repo://app/src/main/java/fr/free/nrw/commons/contributions/MainActivity.kt
generated: { by: "copilot", at: "2026-10-31T14:35:25.887Z" }
---

# Account and authentication flow

## Interactive sign-in

`LoginActivity` collects the username, password, and optional challenge code, then delegates the network exchange to `LoginClient`. The client first requests a MediaWiki login token and submits the credentials with that token. OAuth two-factor, email authentication, password-reset, server errors, and transport failures are reported through `LoginCallback`; after a successful login it requests user information and attaches the user ID and groups to the result.

On success, `LoginActivity` sets the persisted `isUserLoggedIn` flag, asks `SessionManager` to update the Android account, and starts `MainActivity`. The activity can also save and restore credential fields and challenge state across recreation. A user can choose to skip sign-in; that sets the separate `login_skipped` preference and enters the logged-out navigation path, which exposes Explore rather than the signed-in destinations.

## Account and session state

`SessionManager` resolves the current account from Android `AccountManager` using the flavor-specific `ACCOUNT_TYPE`. It reads and stores the password through `AccountManager`, while the Boolean `isUserLoggedIn` flag lives in the default `JsonKvStore`. These values are related but distinct: `LoginActivity.onResume()` returns to the main screen only when both an account exists and the logged-in flag is true.

The app also registers `WikiAccountAuthenticatorService` for Android's account-authenticator action. For the app's account type, its add-account result launches `LoginActivity` with the authenticator response; other account types receive a non-login result. The authenticator's token, credential-update, and confirmation methods currently return placeholder bundles, so do not assume they implement a general Android token broker.

## Cookies and CSRF tokens

MediaWiki and Wikidata Retrofit requests use the persisted cookie jar described in [Networking and local storage](../architecture/networking-and-storage.md). `CsrfTokenClient` obtains a CSRF token for writes. If the asynchronous token request fails or returns an anonymous token while the app believes a user is logged in, it can clear cookies, reauthenticate using the stored account credentials, and retry once. The blocking token path also retries login before giving up; callers should handle `InvalidLoginTokenException` rather than treating an empty token as success.

The cookie-clearing `LogoutClient` used by this retry path only clears cookie storage. It is not the same operation as removing the Android account or clearing all application data.

## Tests

`CsrfTokenClientTest` uses the project's mock web server fixture for token and failure responses. Robolectric tests cover parts of `SessionManager` and the Android authenticator. These tests are useful pointers, but check the production callbacks and state owners directly when behavior has changed; see [Build and test workflow](../testing/build-and-quality.md) for known test-maintenance limits.

## Source-grounded claims

The claims below distinguish account storage, preference state, cookie refresh, and logged-out navigation.
