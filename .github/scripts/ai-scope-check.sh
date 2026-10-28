#!/usr/bin/env bash
set -euo pipefail

provider="${1:?provider is required}"
base_url="${2:-}"
model="${3:-}"
prompt="${4:-}"
api_key="${AI_API_KEY:-}"
title="${PR_TITLE:-}"
body="${PR_BODY:-}"

write_output() {
  local name="$1"
  local value="$2"
  if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    printf '%s=%s\n' "$name" "$value" >> "$GITHUB_OUTPUT"
  fi
}

run_rules() {
  local reason=""
  if [[ -z "${title//[[:space:]]/}" ]]; then
    reason="missing-title"
  elif [[ -z "${body//[[:space:]]/}" ]]; then
    reason="missing-description"
  elif printf '%s\n' "$body" | grep -Eq '^[[:space:]]*[-*+][[:space:]]+\[[[:space:]]\]'; then
    reason="unchecked-checklist"
  fi

  # The rule-based fallback cannot judge whether changes are related, so it
  # never answers "yes" (which would post an "unrelated changes" comment).
  local response="no"
  if [[ -n "$reason" ]]; then
    echo "::notice::Rule-based fallback noticed '${reason}' but cannot judge relevance; no comment will be posted."
  fi
  write_output response "$response"
  write_output mode rules
  write_output reason "$reason"
  printf '%s\n' "$response"
}

case "$provider" in
  none)
    echo "::notice::AI scope check disabled by AI_PROVIDER=none; using the rule-based fallback."
    run_rules
    exit 0
    ;;
  openrouter)
    ;;
  *)
    echo "::error::Unsupported AI_PROVIDER: $provider"
    exit 1
    ;;
esac

if [[ -z "$api_key" ]]; then
  echo "::notice::AI scope check skipped: no API key available"
  run_rules
  exit 0
fi

if [[ -z "$model" ]]; then
  echo "::error::AI scope check requires an OpenRouter model."
  exit 1
fi

base_url="${base_url%/}"
case "$provider:$base_url" in
  openrouter:https://openrouter.ai/api/v1)
    if [[ "$model" != *:free ]]; then
      echo "::error::OpenRouter model ID must end in :free."
      exit 1
    fi
    ;;
  *)
    echo "::error::Unsupported base URL for provider $provider."
    exit 1
    ;;
esac

request_file="$(mktemp)"
response_file="$(mktemp)"
trap 'rm -f "$request_file" "$response_file"' EXIT

jq -n \
  --arg model "$model" \
  --arg prompt "$prompt" \
  --arg title "$title" \
  --arg body "$body" \
  '{
    model: $model,
    messages: [{
      role: "user",
      content: ($prompt + "\n\n<UNTRUSTED_DATA>\nPull Request Title: " + $title +
        "\nPull Request Description: " + $body + "\n</UNTRUSTED_DATA>")
    }]
  }' > "$request_file"

endpoint="$base_url/chat/completions"
http_status="000"
curl_status=0
for attempt in 1 2; do
  if http_status="$(curl -sS -o "$response_file" -w '%{http_code}' \
    --connect-timeout 10 \
    --max-time 60 \
    -H "Authorization: Bearer $api_key" \
    -H 'Content-Type: application/json' \
    --data-binary "@$request_file" \
    "$endpoint")"; then
    curl_status=0
  else
    curl_status=$?
    http_status="${http_status:-000}"
  fi

  if [[ "$curl_status" -eq 0 && "$http_status" =~ ^2[0-9][0-9]$ ]]; then
    break
  fi

  if [[ "$attempt" -eq 1 && "$curl_status" -eq 0 && ( "$http_status" == "429" || "$http_status" =~ ^5[0-9][0-9]$ ) ]]; then
    sleep 2
    continue
  fi
  break
done

fail_with_body() {
  local message="$1"
  local snippet
  snippet="$(head -c 500 "$response_file" | tr '\r\n' '  ' | sed 's/%/%25/g; s/::/%3A%3A/g')"
  echo "::error::$message (HTTP $http_status). Response body (first 500 characters): $snippet"
  exit 1
}

if [[ "$curl_status" -ne 0 || ! "$http_status" =~ ^2[0-9][0-9]$ ]]; then
  fail_with_body "AI scope check request failed"
fi

if ! jq -e '
  type == "object" and
  (.choices | type == "array" and length > 0) and
  (.choices[0].message | type == "object") and
  (.choices[0].message.content | type == "string")
' "$response_file" >/dev/null 2>&1; then
  fail_with_body "AI scope check returned invalid JSON or is missing choices[0].message.content"
fi

answer="$(jq -r '.choices[0].message.content | ascii_downcase | gsub("^\\s+|\\s+$"; "")' "$response_file")"
if [[ "$answer" != "yes" && "$answer" != "no" ]]; then
  fail_with_body "AI scope check returned a response other than yes or no"
fi

write_output response "$answer"
write_output mode ai
printf '%s\n' "$answer"
