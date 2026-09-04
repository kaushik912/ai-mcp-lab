#!/usr/bin/env bash
# Sanity-check your OPENAI_API_KEY works before writing any Python.
# Usage: OPENAI_API_KEY=sk-... ./openai_curl_test.sh
set -euo pipefail

if [ -z "${OPENAI_API_KEY:-}" ]; then
  echo "Set OPENAI_API_KEY first." >&2
  exit 1
fi

curl --location --request POST 'https://api.openai.com/v1/chat/completions' \
  --header 'Content-Type: application/json' \
  --header "Authorization: Bearer ${OPENAI_API_KEY}" \
  --data-raw '{
      "model": "gpt-4o-mini",
      "messages": [
        {"role": "system", "content": "You are a helpful assistant."},
        {"role": "user", "content": "Hello!"}
      ]
    }'
