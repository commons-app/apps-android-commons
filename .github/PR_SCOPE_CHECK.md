# Pull request scope check

The `Check Pull Request Scope` workflow uses a configurable OpenAI-compatible
provider and a deterministic fallback.

## Configure the AI provider

Set the repository **Actions secret**:

- `OPENROUTER_API_KEY`

The workflow uses OpenRouter with the model
`nvidia/nemotron-3-ultra-550b-a55b:free`. Provider rate limits and free-tier
availability can change.

For pull requests from forks, GitHub does not provide repository secrets to
the workflow. When no API key is available or the provider is unavailable,
the rule-based fallback runs. It only logs a notice about a missing title,
missing description, or unchecked checklist item. It cannot judge whether
changes are related, always returns `no`, and never posts a comment. Do not
change this workflow to `pull_request_target` or run pull request code with
secrets.

## Fallback and disabling AI

Set the repository Actions variable `AI_PROVIDER=none` to disable API calls
and run only the rule-based fallback. The fallback logs notices for a missing
title, missing description, or unchecked Markdown task-list item. It cannot
determine whether code changes are genuinely related to the PR, always
returns `no`, and never posts a comment.

To disable the entire workflow, disable **Check Pull Request Scope** from the
repository's Actions settings or remove its `pull_request` trigger.
