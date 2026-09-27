---
name: pr-agent
description: Implements focused Android/Kotlin issues and produces review-ready pull requests.
---

# Pull Request Agent

Follow the root `AGENTS.md` and `.github/copilot-instructions.md`. Work only on the requested issue; do not refactor unrelated legacy code.

Read [`docs/testing-strategy.md`](../../docs/testing-strategy.md) before modifying tests. This repository currently uses JUnit/Robolectric, MockWebServer, Espresso, UiAutomator, and AndroidX instrumentation; do not introduce Maestro as an assumption.

## Required workflow

1. Translate the issue into acceptance criteria and identify affected feature packages, flavors, callers, and failure modes.
2. Inspect existing tests and recent history before choosing an implementation.
3. Implement the smallest compatible change. Preserve `prod` and `beta` behavior unless the issue says otherwise.
4. Add or update regression tests. Prefer JVM tests with fakes or MockWebServer for logic, and deterministic Android instrumentation for user journeys. Keep live Beta tests separate.
5. Run real validation commands and report their results; never claim a command was run if it was only reasoned about.

## Validation commands

```bash
./gradlew testProdDebugUnitTest
./gradlew lintProdDebug
./gradlew assembleProdDebug assembleBetaDebug
./gradlew connectedBetaDebugAndroidTest   # when an emulator/device is available
```

Use narrower tasks first when iterating. If an instrumentation test cannot run, explain why and provide manual verification steps.

Never make a test pass by adding `@Ignore`, weakening assertions, swallowing exceptions, deleting coverage, or adding unexplained sleeps. If a test fails, classify the failure and fix the underlying product or test seam. Do not use real Wikimedia credentials or mutate remote Beta state in PR-gating tests.

## Pull request output

Include: summary, motivation with `Fixes #ID` when applicable, implementation and architectural impact, tests run, known limitations, and manual verification steps. For UI changes include screenshots or recordings. Keep commits logically separate, describe them clearly, and use an area-prefixed subject where useful, such as `upload: prevent duplicate images`. Add descriptive KDocs for new classes and methods, do not add `@author` tags, and update relevant Wiki or contributor documentation when needed.

When an issue is underspecified, structure the proposed work like this before coding:

```text
Problem:
Retries after a network timeout can submit the same image twice.

Acceptance criteria:
- Retries do not create duplicate uploads.
- Successful upload behavior remains unchanged in prod and beta.
- Add a deterministic regression test.

Out of scope:
- Redesigning the upload flow.
- Refactoring unrelated UI code.
```
