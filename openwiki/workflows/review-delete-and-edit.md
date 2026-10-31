---
type: workflow
title: Review, deletion and image editing
description: How peer review picks and tracks files, how deletion nominations are written to Commons pages, and how the in-app editor rotates, crops and blurs images.
tags: [review, deletion, edit, jpegtran, mediawiki]
verified:
  - by: openwiki/0.7.2
    at: 2026-10-31T14:59:24.742Z
sources:
  - id: openwiki-source-3a03962e203b5d1e79132dca
    resource: repo://app/src/main/java/fr/free/nrw/commons/delete/DeleteHelper.kt
  - id: openwiki-source-c3075e25dc304cc99ae2221c
    resource: repo://app/src/main/java/fr/free/nrw/commons/edit/EditActivity.kt
  - id: openwiki-source-2fdc570b5d23d98b9adfe4a1
    resource: repo://app/src/main/java/fr/free/nrw/commons/edit/EditViewModel.kt
  - id: openwiki-source-0ca18ade59d0e2acbf5c444b
    resource: repo://app/src/main/java/fr/free/nrw/commons/review/ReviewHelper.kt
generated: { by: "copilot", at: "2026-10-31T14:59:24.742Z" }
---

# Review, deletion and image editing

Three features let a signed-in user act on files after or before upload. All talk to Commons through the authenticated clients described in [Networking and storage](../architecture/networking-and-storage.md); failures from an expired token surface as `InvalidLoginTokenException` so the UI can send the user back to login (see [Account and authentication](account-and-authentication.md)).

## Peer review

`ReviewHelper` fetches recent changes, shuffles them, and keeps only those whose title ends with a supported image extension. For each candidate it checks whether `Commons:Deletion_requests/<title>` exists; if so the candidate is rejected, otherwise the media record is loaded. `getRandomMedia` returns the first candidate that has a filename and has not already been reviewed.

"Reviewed" is local state: `addViewedImagesToDB` inserts a `ReviewEntity` through `ReviewDao` for both reviewed and skipped images, and `getReviewStatus` consults that table. The helper also exposes the first revision of a file and whether the file is used on any wiki, which the review screen shows to the reviewer.

`ReviewController` performs the reviewer actions (report spam, possible copyright violation, wrong category, and send thanks) and reports back through a callback with success, failure, token-exception and button enable/disable hooks.

## Deletion nominations

`DeleteHelper.makeDeletion` nominates a file by performing four chained page edits, each only if the previous returned true:

1. Prepend a `{{delete|...}}` template to the file page.
2. Create the `Commons:Deletion_requests/<file>` subpage with the reason.
3. Append the subpage to the daily `Commons:Deletion_requests/<date>` log.
4. Append a notice to the creator's user talk page.

A false result at any step raises "Failed to nominate for deletion" and the later steps are skipped, so a nomination can be partially applied. A user notification reports the outcome.

## Image editing

`EditActivity` hosts rotate, crop and blur modes over a preview. `EditViewModel` performs the actual transforms. Rotation and cropping use lossless JPEG operations via Jpegtran and write a new `cropped_<timestamp>.jpg` file instead of modifying the original; failures are logged and rethrown. On save, `EditActivity` applies any pending rotation and returns the edited file path to its caller as `editedImageFilePath` in an activity result; on error it shows a "failed to save" toast and stays open.
