---
type: feature-workflow
title: Nearby places and location
description: Explains how Nearby gets device location, queries Wikidata for places needing photos, enriches and caches place details in parallel batches, and how the location picker sets coordinates for uploads.
tags: [nearby, location, wikidata, maps, places]
verified:
  - by: openwiki/0.7.2
    at: 2026-10-31T14:59:24.742Z
sources:
  - id: openwiki-source-0617ad40edebe7c7fcb616cb
    resource: repo://app/src/main/java/fr/free/nrw/commons/location/LocationServiceManager.kt
  - id: openwiki-source-c952974a69d2ac93da96918b
    resource: repo://app/src/main/java/fr/free/nrw/commons/nearby/NearbyPlaces.java
  - id: openwiki-source-d36862fa87d671e383bf5190
    resource: repo://app/src/main/java/fr/free/nrw/commons/nearby/PlacesRepository.java
  - id: openwiki-source-16bc43d1a20e1deedce55284
    resource: repo://app/src/main/java/fr/free/nrw/commons/nearby/presenter/NearbyParentFragmentPresenter.kt
generated: { by: "copilot", at: "2026-10-31T14:59:24.742Z" }
---

# Nearby places and location

Nearby shows Wikidata items that lack a Commons image around the user or the visible map area, and lets users start an upload tied to a place. It sits beside Explore and Contributions in [Discovery and contribution browsing](discovery-and-contributions.md); the resulting upload and P18 write are in [Media upload lifecycle](media-upload.md) and [Wikidata, categories and depictions](../architecture/wikidata-categories-and-depictions.md).

## Location updates

`LocationServiceManager` registers with the platform `LocationManager` (network and GPS providers) and classifies each fix against the best known one with `isBetterLocation`: a first fix, or a better fix that is 1 km or more away, is `LOCATION_SIGNIFICANTLY_CHANGED`; a better fix less than 1 km away is `LOCATION_SLIGHTLY_CHANGED`; otherwise `LOCATION_NOT_CHANGED`. Listeners (the Nearby presenter among them) receive only the significant and slight callbacks. Location permission handling is in `LocationPermissionsHelper`.

## The single update entry point

`NearbyParentFragmentPresenter.updateMapAndList` is the one place that refreshes markers. It returns without work when Nearby is locked, the network is unavailable, no map centre or last focus is known, or a just-finished internal upload set `isInternalUploadInProgress` (that flag is consumed so the next change is processed). Otherwise it locks Nearby (disabling the recenter button and showing progress) and asks the view to populate places for the custom query, the map centre (significant change or `MAP_UPDATED`), or the focused area (`SEARCH_CUSTOM_AREA`). Slight changes only move the user marker. The lock is released in `updateMapMarkers`, which means overlapping fetches are suppressed rather than queued. A successful Wikidata edit triggers a `MAP_UPDATED` refresh so a newly photographed place leaves the list.

## Querying places

`NearbyController` asks `NearbyPlaces.radiusExpander`, which issues the Wikidata query through `OkHttpJsonApiClient` starting at a 0.3 km radius and doubling until at least 20 places are found or 300 km is reached. The "closest place" variant used by the Contributions nearby card needs only one result within 5 km. Results are sorted by distance from the current point and truncated to `MAX_RESULTS` (1000). A user-supplied advanced query (`AdvanceQueryFragment`) replaces the default SPARQL and can also move the search location.

## Enrichment and cache

The SPARQL result gives location and identity only. `loadPlacesDataAsync` cancels any previous job, then:

1. `loadCachedPlaces` fills each marker from the Room-backed `PlacesRepository`/`PlacesLocalDataSource` when a named cached `Place` exists, and records bookmark state from `BookmarkLocationsDao`.
2. Indices of uncached places are split into batches of 10 and fetched concurrently by 20 coroutine workers calling `nearbyController.getPlaces`.
3. If a batch request fails, each place in it is retried individually because one bad item can make Wikidata fail the whole batch; a place that still fails keeps its partial data.
4. Each fetched place is saved to the cache and merged back, preserving its location, distance and monument flag, and queued clicks or bookmark toggles made while loading are replayed.

Markers are re-rendered incrementally through `schedulePlacesUpdate`, so the map appears first and fills in details. `PlacesRepository.clearCache` empties the cache; `NearbyParentFragment` invokes it as a user-initiated cache clear.

## Location picker

`LocationPickerActivity` lets users pin a map centre. When invoked without a `Media` object (during upload) it returns a `CameraPosition` result; for an existing file it writes the new coordinates through `updateCoordinates`. When launched from the no-location upload flow it also stores the last location and zoom in the key-value store so the next picker opens in the same spot. Users can remove an image's location or open it in an external map app.

## Pitfalls

- Place name and details come from the cache when a cached name exists; stale cache entries can show outdated names until a fetch overwrites them.
- Because refreshes are dropped while locked, every error branch in `NearbyParentFragment`'s populate-places flow must call `lockUnlockNearby(false)` (they currently do); new failure paths must do the same or Nearby stops refreshing.
