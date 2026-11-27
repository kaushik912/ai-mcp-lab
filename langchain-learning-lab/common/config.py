"""Single place to change which model every example uses.

Edit a constant here and every example picks it up automatically - no example
script hardcodes a model name; they all call the `common.models` factory with
no `provider=`/`model=` argument, which falls back to these defaults.
"""
import os

# Which provider common.models.get_chat_model()/get_embeddings_model() use when
# the caller doesn't pass provider= explicitly. "gemini" | "openai" | "openrouter"
# Override per-process with LLL_DEFAULT_PROVIDER (e.g. for scripts/check_examples.py
# to smoke-test a provider without editing this file).
DEFAULT_PROVIDER = os.getenv("LLL_DEFAULT_PROVIDER", "openrouter")

GEMINI_CHAT_MODEL = "gemini-3.5-flash-lite"
GEMINI_EMBEDDING_MODEL = "gemini-embedding-001"
OPENAI_CHAT_MODEL = "gpt-4o"
OPENAI_EMBEDDING_MODEL = "text-embedding-ada-002"
OPENROUTER_CHAT_MODEL = "deepseek/deepseek-v4.1-flash"  # provider/model slug
OPENROUTER_EMBEDDING_MODEL = "qwen/qwen3-embedding-8b"  # alt: qwen/qwen3-embedding-4b
OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"

#Here is how to use OpenRouter's free embedding models—
#nvidia/nemotron-3-embed-1b:free  (EOL 2026-11-23, returns HTTP 410)
#liquid/lfm-2.5-embedding-350m:free
#nvidia/llama-nemotron-embed-vl-1b-v2:free