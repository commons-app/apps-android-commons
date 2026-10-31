---
type: discovery-workflow
title: Discovery and contribution browsing
description: Explains primary navigation, paged Commons browsing, server-backed user contribution history, and Nearby Wikidata discovery with a local place cache.
tags: [discovery, contributions, explore, wikidata, paging]
sources:
  - id: openwiki-source-8124198b272528b78cb50b9f
    resource: repo://app/src/main/java/fr/free/nrw/commons/contributions/ContributionBoundaryCallback.kt
  - id: openwiki-source-db262fef8b4256d5f97aec50
    resource: repo://app/src/main/java/fr/free/nrw/commons/contributions/ContributionsListPresenter.kt
  - id: openwiki-source-913d8b68d6f36385beaf2fbe
    resource: repo://app/src/main/java/fr/free/nrw/commons/contributions/ContributionsRemoteDataSource.kt
  - id: openwiki-source-b99f07c7f59c0fcd00129c17
    resource: repo://app/src/main/java/fr/free/nrw/commons/contributions/MainActivity.kt
  - id: openwiki-source-b98c4159cd8508f834aa826f
    resource: repo://app/src/main/java/fr/free/nrw/commons/explore/ExploreFragment.kt
  - id: openwiki-source-8677c553ea6fae36be1d10ad
    resource: repo://app/src/main/java/fr/free/nrw/commons/explore/ExploreListRootFragment.kt
  - id: openwiki-source-0b2a3cd0c7cefe4676e8d614
    resource: repo://app/src/main/java/fr/free/nrw/commons/explore/media/PageableMediaDataSource.kt
generated: { by: "copilot", at: "2026-10-31T14:59:24.742Z" }
verified:
  - by: openwiki/0.7.2
    at: 2026-10-31T14:59:24.742Z
---

# Discovery and contribution browsing

## Primary navigation

`MainActivity` owns the bottom-level app destinations. Signed-in navigation includes Contributions, Nearby, Explore, and Bookmarks; the logged-out route uses a restricted tab set and starts in Explore. The selected Nearby destination is remembered so the signed-in app can reopen it on a later launch.

Explore is itself a three-tab pager: Featured images, mobile uploads, and a map. The two list tabs wrap category-media fragments and request Commons media through `MediaClient`; the map tab can return to Nearby. Search and paging use separate data sources, so follow the specific feature's caller before changing pagination.

## Contribution history: local cache plus server

The self profile's completed contribution list is backed by the local Room contribution table. `ContributionBoundaryCallback` fetches the signed-in user's media from Commons when the list is empty or reaches its end, converts each media item into a completed `Contribution`, and saves it to the repository. This means the UI observes local persisted records while a boundary callback refreshes or extends that cache from the server.

Other users' profiles take a different path: `ContributionsListPresenter` uses `ContributionsRemoteDataSource` directly and does not attach the persistence boundary callback. Do not treat every profile list as the same local dataset. Pending, paused, and failed uploads are separate persisted contribution states managed by the upload flow; see [Media upload lifecycle](media-upload.md).

## Explore and paged search

`PageableMediaDataSource` maps requested page sizes and offsets to `MediaClient.getMediaListFromSearch`. The shared paging layer emits initial/loading/complete/error states, exposes a retry action for the last request, and reports empty results. Category search has a corresponding source backed by `CategoryClient`; that client filters hidden categories from mapped results.

The general search and paging components are reused across categories and depictions. When changing a query or list behavior, verify both the feature-specific load function and the shared paging lifecycle rather than relying only on adapter tests.

## Nearby places

Nearby is a signed-in-only destination. It queries Wikidata for places without a Commons image, enriches and caches them in parallel batches, and locks itself while a refresh is in flight; the full flow, including location-change classification and the location picker, is in [Nearby places and location](nearby-and-location.md). Explore's map tab can receive the Nearby map's zoom and centre through "Show in Explore" arguments.

## Tests and source verification

Useful focused test entrypoints include `ContributionBoundaryCallbackTest`, `PagingDataSourceTest`, and `NearbyPlacesTest`. These cover representative boundaries but are not a guarantee of full end-to-end behavior; see [Build and test workflow](../testing/build-and-quality.md) for ignored tests and CI's current instrumentation-test configuration.

## Source-grounded claims

The claims below distinguish server-fetched contributions, local persistence, and remote map/search data.
