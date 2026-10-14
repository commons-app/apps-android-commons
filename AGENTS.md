# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android application. Gradle configuration lives in `settings.gradle.kts`, `build.gradle.kts`, and `app/build.gradle.kts`; dependency versions are centralized in `gradle/libs.versions.toml`. Production Kotlin code is under `app/src/main/java/fr/free/nrw/commons`, organized by feature (for example, `media`, `upload`, `category`, and `settings`). Android resources and localized strings are in `app/src/main/res`. JVM tests are in `app/src/test/kotlin`; instrumented tests, when present, belong in `app/src/androidTest`. Build variants include `prod` and `beta` flavors.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper and Java 17.

- `./gradlew assembleProdDebug` — build the production debug APK.
- `./gradlew assembleBetaDebug` — build the beta debug APK.
- `./gradlew testProdDebugUnitTest` — run production-flavor JVM unit tests.
- `./gradlew connectedProdDebugAndroidTest` — run instrumented tests on a connected device or emulator.
- `./gradlew lintProdDebug` — run Android Lint for the production debug variant.
- `./gradlew jacocoTestReport` — generate the configured JaCoCo test coverage report, when the task is available.

## Coding Style & Naming Conventions

Write Kotlin with four-space indentation, clear nullability, and small, readable functions. Follow existing Android/Kotlin conventions: `PascalCase` for classes and composables, `camelCase` for methods and properties, and descriptive `*Test` or `*UnitTest` test classes. Keep feature code in its relevant package and place UI strings in resources rather than hard-coding them. Add reasonably descriptive KDocs for every new class and method; do not add `@author` tags. Readable code should make comments unnecessary except where they explain non-obvious behavior.

## Testing Guidelines

Use the existing JUnit/Robolectric-style unit-test setup for JVM tests and AndroidX instrumentation for device tests. Name tests after the subject and behavior, such as `MediaClientTest` or `TransformGestureDetectorUnitTest`. Add or update tests for behavior changes, and run the narrowest relevant Gradle test task before broader validation.

## Commit & Pull Request Guidelines

Make separate commits for logically separate changes. Use a short first line, optionally prefixed with the affected area (for example, `upload: prevent duplicate images`), followed by a meaningful body when context is needed. Pull requests should explain the problem and solution, include testing performed, link related issues, and provide screenshots or recordings for UI changes. Update relevant documentation or wiki content when behavior changes, and keep the Wiki current when a contribution changes contributor-facing instructions.

## Security & Configuration

Never commit passwords, API credentials, signing keys, decrypted keystores, or generated release artifacts. Review `SECURITY.md` before reporting or handling vulnerabilities, and use environment variables/local configuration for signing and test credentials.

## Agent Workflow

Treat this file as the shared baseline for Codex, Claude, Cursor, and Copilot. Read the relevant feature code, tests, Gradle configuration, and recent history before editing. Keep changes narrowly scoped, preserve existing public behavior unless the task says otherwise, and do not “fix” unrelated legacy code. For behavior changes, add a regression test or document why the behavior cannot yet be automated. Before handoff, run the narrowest relevant test, then `./gradlew lintProdDebug` and the affected build variant; report skipped checks and their reason.

GitHub Copilot cloud agent can be assigned issues from GitHub and will open a pull request. Issues should state the user-visible behavior, affected flavor, acceptance criteria, and required tests. Select `.github/agents/pr-agent.agent.md` for implementation or `.github/agents/architecture-reviewer.agent.md` for a focused review task. Reviewers should inspect architecture and failure modes—not only compilation or style—and require manual verification steps for flows not covered by automation.

Agents must never make a test pass by adding `@Ignore`, weakening assertions, swallowing exceptions, deleting coverage, or increasing a fixed sleep without identifying the race. Classify failures as product, test, environment/toolchain, external-service/credential, or flaky-test failures. Keep live Wikimedia/Beta calls and tests that mutate remote state out of the normal PR gate; prefer fakes, test repositories, and MockWebServer for deterministic tests. See [`docs/testing-strategy.md`](docs/testing-strategy.md) for the test layers and escalation path.

## Architecture Contract

- Use a feature-oriented modular-monolith approach. Do not introduce a new architecture pattern or module unless it solves a named problem and has a migration boundary.
- Keep UI entry points focused on rendering state and forwarding events. Do not place business rules, API calls, database queries, upload policy, or retry policy in Activities, Fragments, adapters, or composables.
- Keep domain logic independent of Retrofit, Room, Android `Context`, `View`, `Activity`, `Fragment`, and API DTO types.
- Prefer dependency inversion at external boundaries: define focused interfaces for authentication, Wikimedia APIs, persistence, uploads, filesystem access, location, and background work.
- Keep one source of truth per feature and prefer one-way state flow: user event → presenter/ViewModel → domain operation → repository → state/result → UI.
- Map external DTOs and database entities into feature/domain models before exposing them to UI code.
- Represent meaningful concepts with validated models or value objects rather than unstructured `String`, `Boolean`, or map values.
- Define retryable, permanent, offline, authentication-expired, and duplicate-operation behavior explicitly.
- For uploads and workers, reason about cancellation, retries, process death, idempotency, and duplicate submissions.
- Prefer realistic fakes, test repositories, fixtures, and MockWebServer over tests that only verify mock call sequences.
- Apply SOLID where it reduces coupling; do not add interfaces, wrappers, use cases, or domain events solely for ceremony.

Example boundary:

```kotlin
// Prefer: domain-facing interface
interface UploadGateway {
    suspend fun upload(request: UploadRequest): UploadResult
}

// Avoid: domain code depending directly on a Retrofit API
class UploadUseCase(private val uploadApi: UploadApi)
```

Example test seam: `UploadUseCaseTest → FakeUploadGateway`. Avoid `UploadActivityTest → real Wikimedia API → real account → remote upload`.

## Android Documentation Policy

Use the current official documentation as the normative source for Android platform, Gradle, Kotlin, Compose, lifecycle, permissions, accessibility, security, and testing guidance:

- [Android Developers](https://developer.android.com/)
- [Android app architecture](https://developer.android.com/topic/architecture)
- [Android testing](https://developer.android.com/training/testing)
- [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)

Check the relevant official page before introducing or recommending an API, pattern, or workaround. Prefer current stable APIs and document any deliberate compatibility decision for this app's `minSdk = 21`. The Commons documentation repository is useful project context, but it may be older or contain drafts; it must not override current official Android guidance without an explicit project decision.
