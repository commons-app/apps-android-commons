```markdown
# agents.md — guidance for AI/code generation and PRs

Purpose
- Help people and AI assistants (e.g., GitHub Copilot) create useful, high-quality pull requests for this repository.

When to use
- Use this guidance for bots, Copilot-style completions, or when drafting PRs from prompts.

Top-level guidance
- Keep changes small and scoped to a single logical issue.
- Always include a clear issue reference (e.g. "Fixes #6564") when the change resolves or implements an open issue.
- Write human-readable commit messages and PR descriptions that explain why (not just what) changed.

What to include in a PR
- Summary: 1–2 sentence summary of the change.
- Motivation: Why is this change needed?
- Implementation: Short explanation of the approach taken.
- Screenshots / GIFs for UI changes.
- Tests added/updated (if applicable).
- How to test: step-by-step reproduction and verification steps.
- Checklist: linting, unit tests, instrumentation tests, built locally.

Writing prompts for this repository
- Provide the file or path you want to change.
- Include the repository language and frameworks (Kotlin, Android).
- State the exact behavior expected (before/after).
- Provide tests or sample inputs if relevant.

Examples of good prompts
- "Add a .github/agents.md file that explains how to ask Copilot to create small PRs with: summary, motivation, implementation, test steps, and issue reference."
- "Implement small UI fix: change text size in the settings screen to 14sp for the subtitle. Provide a unit test and steps to manually verify."

Do / Don't
- Do: Keep PRs focused and small; include tests or manual steps.
- Do: Reference the issue number.
- Don't: Make sweeping refactors in the same PR as a bugfix.
- Don't: Omit test or verification instructions for behavior changes.

Maintainers’ expectations
- CI must pass on the PR.
- If requested, follow up quickly on requested changes.
- Welcome contribution notes: say how maintainers can reproduce the issue and confirm the fix.

Helpful links
- GitHub guidance: https://github.blog/ai-and-ml/github-copilot/how-to-write-a-great-agents-md-lessons-from-over-2500-repositories/
```