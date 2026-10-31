---
type: "Reference"
title: "Wikidata, categories and depictions"
openwiki_generated: true
verified:
  - by: openwiki/0.7.2
    at: 2026-10-31T14:59:24.742Z
sources:
  - id: openwiki-source-7ebfe646beae36d3eb2a7aab
    resource: repo://app/src/main/java/fr/free/nrw/commons/category/CategoriesModel.kt
  - id: openwiki-source-0af3837ce8f20f89e37f2ed1
    resource: repo://app/src/main/java/fr/free/nrw/commons/category/CategoryEditHelper.kt
  - id: openwiki-source-e19c64337ebc1d00b4047d04
    resource: repo://app/src/main/java/fr/free/nrw/commons/upload/worker/UploadWorker.kt
  - id: openwiki-source-b72217f31758a4e9bd54336c
    resource: repo://app/src/main/java/fr/free/nrw/commons/wikidata/WikidataClient.kt
  - id: openwiki-source-3b0e70723597a7d28bf1a3ac
    resource: repo://app/src/main/java/fr/free/nrw/commons/wikidata/WikidataEditService.kt
generated: { by: "copilot", at: "2026-10-31T14:59:24.742Z" }
---


# Wikidata, categories and depictions

Three separate MediaWiki surfaces describe one Commons file, and each has its own client and failure mode. This page is the map; the upload sequencing is in [Media upload lifecycle](../workflows/media-upload.md) and the transports are in [Networking and local storage](networking-and-storage.md).

| Surface | Written by | Mechanism |
|---|---|---|
| Structured data on the file (depicts, captions) | `WikidataEditService` via `WikiBaseClient` | Wikibase entity edits on `M<pageId>` |
| P18 image statement on a Wikidata place | `WikidataEditService` via `WikidataClient` | `wbsetclaim` with a CSRF token |
| Categories | `CategoryEditHelper` (existing files) or upload page text | Rewritten wikitext via `PageEditClient` |

## Depicts and captions on the file entity

After the final upload succeeds, `UploadWorker` calls `WikidataEditService.addDepictionsAndCaptions` and blocks on it. The service first resolves the file's entity id from the upload result, then concatenates a depicts edit and one caption edit per entry of `contribution.media.captions`. If the entity id cannot be resolved the observable is empty and no structured data is written, but the upload itself is not failed by this.

Depicts items come from `contribution.depictedItems`. In the beta flavor the service substitutes the single item `Q10` regardless of selection, because production Wikidata ids do not exist on the beta site. `updateDepictsProperty` (used when editing an existing file) first looks up the file's existing depicts claim ids, deletes them, and only on a successful delete adds the new set; a failed delete yields an empty stream rather than a partial state. Errors surface as long toasts, not exceptions to the caller.

## P18 claims for Nearby places

When a contribution carries a `wikidataPlace` (it was started from a Nearby place), `UploadWorker.makeWikiDataEdit` runs after the file is stored. It skips the Wikidata write when the contribution has an invalid location, and skips it when the place already has an image value, so an existing P18 is never overwritten. Otherwise `createClaim` first checks the `Picture_Has_Correct_Location` preference (a location mismatch cancels the edit) and then builds a P18 statement whose value is the unique uploaded filename without the `File:` prefix, with media-legend qualifiers for each caption. The worker deliberately uses the post-collision unique name rather than MediaWiki's underscored canonical name, because the latter triggers Wikidata's malformed-link warning. `WikidataClient.setClaim` fetches a CSRF token, posts the claim with the `wikimedia-commons-app` edit tag, and returns the revision id. On success the local cached `Place` row is updated with the new picture, and `WikidataEditListener` is notified. The contribution is recorded complete whether or not this optional edit succeeded.

## Categories

Category browsing and search use `CategoryClient`, a continuation-based wrapper over MediaWiki query APIs: prefix and substring search, lookup by name range, categories of a given image, and paged sub/parent category lists (continuation state is keyed by category and can be reset). During upload, `CategoriesModel.searchAll` merges suggestions when the search box is empty (categories from selected depictions, from GPS location, from the image title, and recently used categories) and otherwise runs a prefix search sorted by similarity to the term.

For files that already exist, `CategoryEditHelper.makeCategoryEdit` re-writes the page text: it keeps everything before the first `Uncategorized` marker or `[[Category` link, appends a `[[Category:...]]` line for each selected category (dropping the placeholder `None selected`), or appends `{{subst:unc}}` when none are chosen, then posts the edit with the summary "Adding categories". The result is reported through a notification linking to the file page. Because the edit replaces the page text from the first category marker onward, text placed after categories in the wikitext would be discarded.

## Extension seams and caveats

- New Wikibase operations belong on `WikiBaseClient`/`WikidataInterface`, which handle CSRF tokens; reuse them instead of creating new Retrofit paths.
- Depiction search and entity lookup live in `explore/depictions` (`DepictsClient`) and feed both Explore and the upload depiction step.
- The worker invokes these edits with `blockingSubscribe`/`blockingSingle` on its background thread, so slow Wikidata calls extend upload duration.
