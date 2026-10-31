---
type: upload-workflow
title: Media upload lifecycle
description: Traces selected media through the upload editor, durable queued contributions, WorkManager processing, resumable MediaWiki stash chunks, and completion or failure.
tags: [upload, workmanager, mediawiki, wikidata, persistence]
verified:
  - by: openwiki/0.7.2
    at: 2026-10-31T14:35:25.887Z
sources:
  - id: openwiki-source-186e96b8d6739f3745947903
    resource: repo://app/src/main/AndroidManifest.xml
  - id: openwiki-source-096d6e722e708edd1016a52c
    resource: repo://app/src/main/java/fr/free/nrw/commons/contributions/ContributionController.kt
  - id: openwiki-source-c4b00c32c6f7548b62ecf7b5
    resource: repo://app/src/main/java/fr/free/nrw/commons/upload/UploadActivity.kt
  - id: openwiki-source-3112117c59fe00b392555ff4
    resource: repo://app/src/main/java/fr/free/nrw/commons/upload/UploadClient.kt
  - id: openwiki-source-0ca361ccb7b48b866a77fcf3
    resource: repo://app/src/main/java/fr/free/nrw/commons/upload/UploadPresenter.kt
  - id: openwiki-source-e19c64337ebc1d00b4047d04
    resource: repo://app/src/main/java/fr/free/nrw/commons/upload/worker/UploadWorker.kt
  - id: openwiki-source-a48bb1f5dc611574f6623c22
    resource: repo://app/src/main/java/fr/free/nrw/commons/upload/worker/WorkRequestHelper.kt
generated: { by: "copilot", at: "2026-10-31T14:35:25.887Z" }
---

# Media upload lifecycle

## Selection and editing

Media can enter from the app's camera/gallery/custom selector or through Android share intents. `ContributionController` converts files selected in-app into an internal intent for `UploadActivity`, including the selected `UploadableFile` list and optional nearby-place or captured-location context. The manifest also lets `UploadActivity` receive external image/audio `SEND` and `SEND_MULTIPLE` intents.

`UploadActivity` resolves those inputs, creates media-detail fragments for each selected file, and then adds shared depiction, category, and license steps. The per-file editor also handles metadata, location, and thumbnail changes. Removing the last file from the editor displays cancellation and finishes that activity; this is cancellation before an upload contribution has been queued.

## Queue creation

The submit action reaches `UploadPresenter`. Submission requires an authenticated session; otherwise the presenter asks the user to log in. For an authenticated user, the upload model builds `Contribution` records from the selected files and metadata, media is prepared, each record is marked `STATE_QUEUED`, and it is saved through `UploadRepository`/`ContributionDao`. Once the sequence completes, the activity enqueues upload work and navigates to upload progress.

The persisted contribution contains the local media reference, metadata, categories, depictions, location/place information, and upload state. `ContributionDao` owns the durable state and timestamps, so the background worker can continue after `UploadActivity` is gone. Limited-connection mode changes the submit message; actual WorkManager execution still requires a connected network.

## Background processing and remote upload

`WorkRequestHelper` creates unique one-time `UploadWorker` work with a connected-network constraint and linear backoff. The worker processes queued contributions sequentially, ordered by upload start time, and persists the transition to `STATE_IN_PROGRESS`. It updates progress and chunk metadata through the contribution DAO.

`UploadClient` transfers media to the MediaWiki upload stash in 512 KiB chunks. It stores the returned chunk index, offset, and stash key on the contribution, allowing the client to resume from a valid stash for up to six hours. A missing/deleted contribution is treated as cancelled; pause and transfer failures remain distinct outcomes.

After a stash succeeds, the worker chooses a unique filename, submits the completed file with a CSRF token and generated page contents, and makes Wikidata edits when the contribution carries a place or depiction information. It then fetches the uploaded media record and replaces the queued record with a completed contribution. Media-upload failures are surfaced through notifications and persisted failure state. Optional place edits are attempted separately; the worker catches those exceptions and can still mark the media contribution completed. An invalid login also triggers application-data cleanup and a return to login.

## Cancellation and tests

There are different cancellation points: removing media from the pre-submit editor is handled by `UploadPresenter`; a queued or running contribution is represented in the database and checked by the worker/client. Do not equate the editor's last-image cancellation with cancellation of an already-started remote upload.

`UploadClientTest` covers selected stash-call success/error behavior and upload request fields. `UploadPresenterTest` has some ignored cases, so verify test assumptions against the current presenter and worker source; see [Build and test workflow](../testing/build-and-quality.md) for broader test limitations.

## Source-grounded claims

Claims below follow the current queue and worker implementation; nearby test cases are supporting evidence only.
