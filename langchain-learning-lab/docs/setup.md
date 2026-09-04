# Setup

## Python environment

See the repo root `README.md` for the venv + `pip install -r requirements.txt` steps. This page covers the pieces `pip` can't install: your IDE and provider API keys.

## PyCharm (optional)

These notes were originally written using PyCharm Community Edition. Any IDE with a Python virtualenv integration works fine — this repo doesn't depend on PyCharm specifically.

## OpenAI setup

1. Create an OpenAI account and add credits.
2. Generate an API key at [platform.openai.com](https://platform.openai.com/).
3. Test it: `examples/01_first_programs/openai_curl_test.sh` (expects HTTP 200).
4. Put the key in `.env` (see repo root `.env.example`) — never hardcode it or export it inline where it could leak into shell history/CI logs long-term.

## Gemini setup

1. Generate an API key at [Google AI Studio](https://aistudio.google.com/apikey) — it has a free tier, no credit card required.
2. Put it in `.env` as `GEMINI_API_KEY` (see repo root `.env.example`).
3. Most examples default to Gemini (`gemini-flash-lite-latest` for chat, `gemini-embedding-001` for embeddings) since it needs no local model download and no paid credits to try.
