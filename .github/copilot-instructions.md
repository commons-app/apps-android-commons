# Commons Android Copilot Instructions

Follow the repository root `AGENTS.md` for build commands, architecture, testing, and PR rules. These instructions add only what a pull-request review needs. Test layers and CI status are in [`docs/testing-strategy.md`](../docs/testing-strategy.md).

## Review expectations

- Flag only actionable issues, prioritizing: incorrect behavior, lifecycle/state bugs, concurrency, data loss, security/privacy, offline/error handling, API compatibility, performance, architectural boundary violations, and missing regression coverage.
- Trace changes across callers, persistence, workers, APIs, and both `prod` and `beta` flavors.
- Check whether domain logic depends directly on Android, Retrofit, Room, or API DTO types.
- Check whether retries, cancellation, rotation, process death, or duplicate submissions change behavior.
- Require a deterministic test seam for new external dependencies.
- Check that new classes and methods have KDocs and no `@author` tags.
- Check that the PR description links the issue, lists the tests actually run with their results, and includes screenshots or recordings for UI changes.
- Do not report formatting already enforced by the repository.
- Explain the failure scenario, affected code path, and a concrete fix for every finding.

Example finding:

```text
[P1] Duplicate upload possible if the worker re-runs before state is saved

UploadWorker returns Result.retry() when contributions remain queued
(app/src/main/java/fr/free/nrw/commons/upload/worker/UploadWorker.kt:259).
If the contribution state is not persisted before the upload starts, a
re-run can submit it again. Persist STATE_IN_PROGRESS first, and cover the
re-run path with a deterministic test.
```

