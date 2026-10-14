# Android Testing Strategy

This repository uses existing JUnit/Robolectric, MockWebServer, Espresso, UiAutomator, and AndroidX instrumentation infrastructure. Maestro is not part of the current testing baseline.

Tests are also an architecture check: domain rules should be runnable on the JVM, external systems should be replaceable by fakes or fixtures, and UI tests should verify user-visible behavior rather than implementation-specific mock interactions.

## Test layers

| Layer | Scope | Dependencies | Expected trigger |
| --- | --- | --- | --- |
| JVM tests | Models, presenters, clients, persistence, upload logic | Fakes, fixtures, MockWebServer | Every pull request |
| Emulator smoke tests | Login screen, navigation, permissions, orientation, critical screen behavior | Deterministic test session and local fixtures | Every pull request once stable |
| Beta integration tests | Real authentication, Wikimedia API compatibility, upload behavior | Dedicated Beta credentials and remote state | Manual or scheduled workflow |
| Release validation | Production-like build and critical user journeys | Release candidate environment | Release candidate |

## Rules for reliable tests

- Do not require live Wikimedia services or mutable remote state in PR-gating tests.
- Prefer realistic fake repositories and test implementations over verifying mock call sequences.
- Use MockWebServer for HTTP behavior and reset preferences/database state between tests.
- Replace fixed sleeps with Espresso synchronization, explicit polling with timeouts, or testable worker/network state.
- Do not catch and ignore `NoMatchingViewException` or other setup/assertion failures.
- Do not add `@Ignore`, weaken assertions, or suppress a test to make CI pass.

Prefer `UploadUseCaseTest → FakeUploadGateway`. Avoid `UploadActivityTest → real Wikimedia API → real account → remote upload` for pull-request validation.

## Commands

```bash
./gradlew testProdDebugUnitTest
./gradlew lintProdDebug
./gradlew assembleProdDebug assembleBetaDebug
./gradlew connectedBetaDebugAndroidTest   # when an emulator/device is available
```

Use a focused test class or method while iterating. Record the exact variant, emulator/API level, backend, credentials mode, and command for every result. Classify failures as product, test, environment/toolchain, external-service/credential, or flaky-test failures before changing code.

## Stabilization order

When an instrumentation test is failing, first make the failure observable, then remove timing and external-service dependencies, then add deterministic fixtures or a fake session. Promote one stable test group to the PR gate at a time. Keep upload and other remote-mutating tests outside the PR gate until isolation and cleanup are proven.

For contributor-facing test changes, follow `CONTRIBUTING.md`: keep commits logically separate, describe the change clearly, add tests where possible, and update the Wiki or related documentation when the testing workflow changes.
