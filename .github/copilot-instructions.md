# Commons Android Copilot Instructions

Use the repository root `AGENTS.md` as the source of truth. Custom Copilot agents are defined in `.github/agents/*.agent.md`.

Keep these instructions concise and actionable. Prefer short imperative bullets, concrete examples, and distinct headings. Do not add vague goals such as “improve quality” or instructions about Copilot’s response formatting. Copilot code review is non-deterministic and context-limited; prioritize the highest-value rules and iterate using real pull requests.

Read [`docs/testing-strategy.md`](../docs/testing-strategy.md) before changing tests. This repository uses JUnit, Robolectric, MockWebServer, Espresso, UiAutomator, and AndroidX instrumentation; Maestro is not part of the current baseline.

## Documentation authority

Use current Android and Kotlin platform guidance when making implementation decisions. Use Commons documentation for project context, not as a substitute for current platform guidance. Explain any `minSdk = 21` compatibility workaround in the PR. The review rules in this file are self-contained; do not depend on following external links.

## Before changing code

- Identify the affected feature package, data/API boundaries, persistence, background work, and UI entry points.
- Read existing tests and the relevant flavor configuration (`prod` or `beta`); do not assume `assembleDebug` or `testDebugUnitTest` exists.
- Preserve compatibility with the app's minimum SDK and both product flavors unless the issue explicitly narrows scope.
- Determine whether the change belongs in JVM tests, deterministic emulator smoke tests, or live Beta integration tests.

## Implementation expectations

- Prefer the smallest change that fits the existing architecture. Do not introduce a new framework, module, or abstraction without explaining its ownership and migration path.
- Keep network, storage, and Android framework calls behind testable boundaries where practical. Avoid real network calls in unit tests.
- For functional changes, add a regression test. For UI or navigation changes, add an Android instrumentation test when feasible and include manual verification if automation is not yet possible.
- Do not weaken tests, suppress lint, alter credentials, or delete existing behavior to make CI pass.
- Do not use live Wikimedia accounts or remote mutable state for PR tests. Use fake sessions, test repositories, fixtures, or MockWebServer.
- Replace fixed sleeps and swallowed `NoMatchingViewException` failures with synchronization and explicit diagnostics.
- Add descriptive KDocs for every new class and method; do not add `@author` tags.
- Keep documentation and relevant Wiki pages current when behavior or contributor workflow changes.

## Review expectations

- Flag only actionable issues, prioritizing: incorrect behavior, lifecycle/state bugs, concurrency, data loss, security/privacy, offline/error handling, API compatibility, performance, architectural boundary violations, and missing regression coverage.
- Trace changes across callers, persistence, workers, APIs, and both product flavors.
- Check whether domain logic depends directly on Android, Retrofit, Room, or API DTOs.
- Check whether retries, cancellation, rotation, process death, or duplicate submissions change behavior.
- Require a deterministic test seam for new external dependencies.
- Do not report formatting preferences already enforced by the repository.
- Explain the failure scenario, affected users or code path, and concrete fix for every finding.
- Check that new classes and methods have descriptive KDocs and no `@author` tags.
- Check that the PR description links the issue, records tests, and includes UI screenshots or recordings when applicable.

Example finding:

```text
[P1] Retry can upload the same file twice

UploadWorker retries after a timeout, but the server may have accepted the
previous request. Add an idempotency key or verify the existing upload before
retrying, then cover the timeout/retry path with a deterministic test.
```

## Verification

Run the narrowest relevant Gradle task, then `./gradlew lintProdDebug` and the affected build task. If an emulator is available, run `./gradlew connectedBetaDebugAndroidTest`; otherwise state that instrumentation was not run and provide exact manual steps.

Never claim a command was run if it was only reasoned about. Report the exact variant, device/API level, credentials or backend used, failed test class, and failure category.
