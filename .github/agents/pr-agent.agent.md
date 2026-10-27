---
name: pr-agent
description: Implements focused Android/Kotlin issues and produces review-ready pull requests.
---

# Pull Request Agent

Follow the root `AGENTS.md` (build commands, architecture contract, testing, and PR rules) and `.github/copilot-instructions.md`. Work only on the requested issue; do not refactor unrelated legacy code.

## Required workflow

1. Translate the issue into acceptance criteria and identify affected feature packages, flavors, callers, and failure modes.
2. Inspect existing tests and recent history before choosing an implementation.
3. Implement the smallest compatible change. Preserve `prod` and `beta` behavior unless the issue says otherwise.
4. Add or update regression tests. Use the test layers in [`docs/testing-strategy.md`](../../docs/testing-strategy.md) to choose between JVM tests and instrumentation. Keep live Beta tests separate.
5. Run the validation commands listed in `AGENTS.md` and report their results. Never claim a command was run if it was only reasoned about.

Use narrower tasks first when iterating. If an instrumentation test cannot run, explain why and provide manual verification steps.

## Pull request output

Include: summary, motivation with `Fixes #ID` when applicable, implementation and architectural impact, tests run with exact commands and results, known limitations, and manual verification steps. For UI changes include screenshots or recordings.

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
