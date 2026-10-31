---
type: workflow
title: Profile, settings, bookmarks and notifications
description: How the profile tabs, settings screen, bookmarks pager, Wikimedia notifications, active campaign lookup and revert-rate quiz prompt work in the Commons app.
tags: [profile, settings, bookmarks, notifications, campaigns, quiz]
verified:
  - by: openwiki/0.7.2
    at: 2026-10-31T14:59:24.742Z
sources:
  - id: openwiki-source-16efd1051486043ab1c72703
    resource: repo://app/src/main/java/fr/free/nrw/commons/bookmarks/BookmarksPagerAdapter.kt
  - id: openwiki-source-a49455ed76a59c23969118bb
    resource: repo://app/src/main/java/fr/free/nrw/commons/campaigns/CampaignsPresenter.kt
  - id: openwiki-source-f2ad3e56ef84c34c9b0a56e9
    resource: repo://app/src/main/java/fr/free/nrw/commons/notification/NotificationClient.kt
  - id: openwiki-source-8a8674375673dd77c8e41f6f
    resource: repo://app/src/main/java/fr/free/nrw/commons/profile/ProfileActivity.kt
  - id: openwiki-source-1c37f609af0adb797f8398b3
    resource: repo://app/src/main/java/fr/free/nrw/commons/quiz/QuizChecker.kt
  - id: openwiki-source-334fda2f21499ce7bb22421b
    resource: repo://app/src/main/java/fr/free/nrw/commons/settings/SettingsFragment.kt
generated: { by: "copilot", at: "2026-10-31T14:59:24.742Z" }
---

# Profile, settings, bookmarks and notifications

These are the user-facing "account around the edges" features. Each is a small, mostly independent slice; the shared constraint is that several of them behave differently when the user skipped login (`login_skipped`, see [Account and authentication](account-and-authentication.md)).

## Profile

`ProfileActivity` is a tabbed container. It builds three fragments, each given the target username as an argument: Achievements, Leaderboard, and Contributions. Because the username is a parameter, the same activity serves both the signed-in user and other users' profiles; the Contributions tab then takes the self or other-user data path described in [Discovery and contributions](discovery-and-contributions.md).

## Settings

`SettingsFragment` is a preference screen that wires language lists (app UI language and default description language), theme, deletion-button visibility, beta tester and log-sending actions, and the document-based picker toggle. Turning the document-based picker off shows a location-loss warning. When `login_skipped` is true the fragment disables the preferences that only make sense for an account (external storage, author name, nearby card, description language, campaigns, EXIF tags, picker, in-app camera location, account vanishing); they are disabled, not hidden.

## Bookmarks

`BookmarksPagerAdapter` builds the bookmark tabs. Pictures is always present; when the user is logged in it also adds Locations and Items tabs. The `onlyPictures` flag exists for the logged-out case.

## Notifications

`NotificationClient` queries the Wikimedia notifications API across `wikidatawiki|commonswiki|enwiki`, using the `read` filter for archived and `!read` for unread items, and maps each to the app's own `Notification` model (an "email" type is inferred from the header text). Marking as read needs a CSRF token; an `InvalidLoginTokenException` is propagated so callers can force re-login, while other failures degrade to `false`.

## Campaigns

`CampaignsPresenter` fetches the campaign list via `OkHttpJsonApiClient`, sorts by start date, and picks the first campaign whose start/end window contains today. The result is cached in the presenter, so repeat calls do not hit the network; an empty list or no active campaign shows nothing.

## Revert-rate quiz

`QuizChecker` fetches the user's upload count and revert count. If the upload count meets a threshold and reverts are at least 50% of uploads, it prompts the user to take the quiz, and records the counts in a key-value store so the same uploads are not counted again. Negative counts (fetch failure) reset the stored counters.
