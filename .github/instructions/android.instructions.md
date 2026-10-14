---
applyTo: "**/*.{kt,kts,java,xml}"
---

# Android and Kotlin Development Rules

Use current official Android and Kotlin documentation as the normative source. Follow the repository root `AGENTS.md` for project-specific architecture and testing rules.

## Boundaries

- Keep Activities, Fragments, adapters, and composables focused on UI state and events.
- Keep business rules out of UI classes, Retrofit interfaces, Room DAOs, and Workers.
- Keep domain logic independent of Android framework types, Retrofit DTOs, and Room entities.
- Prefer focused interfaces at external boundaries when they improve testability or isolate a dependency.

## State and reliability

- Preserve one source of truth and one-way state flow per feature.
- Define loading, empty, success, and error states where applicable.
- Consider lifecycle cancellation, rotation, process death, offline mode, retries, authentication expiry, and duplicate operations.
- Use test doubles or MockWebServer instead of real services in JVM and PR-gating tests.

## Maintainability

- Apply SOLID and DDD concepts only when they solve a concrete coupling or domain-model problem.
- Do not add wrappers, interfaces, use cases, modules, or domain events solely for ceremony.
- Add a deterministic regression test for meaningful behavior changes.
