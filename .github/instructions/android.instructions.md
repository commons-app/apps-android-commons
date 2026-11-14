---
applyTo: "**/*.{kt,kts,java,xml}"
---

# Android and Kotlin Development Rules

Apply these to Kotlin, Gradle Kotlin, Java, and XML files in addition to the root `AGENTS.md` (architecture, testing, and PR rules). Use current official Android and Kotlin documentation as the normative source for platform APIs.

- Model loading, empty, success, and error states explicitly in UI-facing code.
- Consider lifecycle cancellation, rotation, process death, offline mode, and retries when adding coroutines, workers, or observers.
- Check the `minSdk = 21` compatibility of any newer platform API before use, and document a deliberate compatibility decision in the PR.
