"""Shared env/key/client helpers so examples don't repeat boilerplate."""
import os
from pathlib import Path

from dotenv import load_dotenv

from common.config import (
    GEMINI_CHAT_MODEL,
    GEMINI_EMBEDDING_MODEL,
    OPENAI_CHAT_MODEL,
    OPENAI_EMBEDDING_MODEL,
    OPENROUTER_BASE_URL,
    OPENROUTER_CHAT_MODEL,
    OPENROUTER_EMBEDDING_MODEL,
)

REPO_ROOT = Path(__file__).resolve().parent.parent
_loaded = False


def load_env() -> None:
    """Load .env once. Safe to call even if no .env exists yet."""
    global _loaded
    if not _loaded:
        load_dotenv(REPO_ROOT / ".env")
        _loaded = True


def get_openai_key(required: bool = True) -> str | None:
    load_env()
    key = os.getenv("OPENAI_API_KEY")
    if required and not key:
        raise RuntimeError(
            "OPENAI_API_KEY not set. Copy .env.example to .env and fill it in."
        )
    return key


def get_openai_chat(model: str = OPENAI_CHAT_MODEL, **kwargs):
    from langchain_openai import ChatOpenAI

    return ChatOpenAI(model=model, api_key=get_openai_key(), **kwargs)


def get_openai_embeddings(model: str = OPENAI_EMBEDDING_MODEL, **kwargs):
    from langchain_openai import OpenAIEmbeddings

    return OpenAIEmbeddings(model=model, api_key=get_openai_key(), **kwargs)


def get_gemini_key(required: bool = True) -> str | None:
    load_env()
    key = os.getenv("GEMINI_API_KEY")
    if required and not key:
        raise RuntimeError(
            "GEMINI_API_KEY not set. Copy .env.example to .env and fill it in."
        )
    return key


def get_gemini_chat(model: str = GEMINI_CHAT_MODEL, **kwargs):
    from langchain_google_genai import ChatGoogleGenerativeAI

    kwargs.setdefault("max_retries", 6)  # retries transient errors with exponential backoff
    kwargs.setdefault("timeout", 60)  # seconds to wait for a response
    return ChatGoogleGenerativeAI(model=model, google_api_key=get_gemini_key(), **kwargs)


def get_gemini_embeddings(model: str = GEMINI_EMBEDDING_MODEL, **kwargs):
    from langchain_google_genai import GoogleGenerativeAIEmbeddings

    return GoogleGenerativeAIEmbeddings(model=model, google_api_key=get_gemini_key(), **kwargs)


def get_openrouter_key(required: bool = True) -> str | None:
    load_env()
    key = os.getenv("OPENROUTER_API_KEY")
    if required and not key:
        raise RuntimeError(
            "OPENROUTER_API_KEY not set. Copy .env.example to .env and fill it in."
        )
    return key


def get_openrouter_chat(model: str = OPENROUTER_CHAT_MODEL, **kwargs):
    from langchain_openrouter import ChatOpenRouter

    get_openrouter_key()  # validate early; ChatOpenRouter itself reads the env var
    return ChatOpenRouter(model=model, **kwargs)


def get_openrouter_embeddings(model: str = OPENROUTER_EMBEDDING_MODEL, **kwargs):
    # OpenRouter's embeddings endpoint is OpenAI-compatible - reuse ChatOpenAI's
    # embeddings class pointed at OpenRouter's base URL instead of OpenAI's.
    from langchain_openai import OpenAIEmbeddings

    # OpenAIEmbeddings pre-tokenizes input into token-ID arrays via tiktoken by
    # default (check_embedding_ctx_length=True); OpenRouter's embedding models
    # (e.g. Nvidia Nemotron) reject that and want raw strings, so disable it.
    kwargs.setdefault("check_embedding_ctx_length", False)
    # It also defaults to requesting base64-encoded vectors, which OpenRouter's
    # embedding models reject - ask for plain floats instead.
    kwargs.setdefault("encoding_format", "float")
    return OpenAIEmbeddings(
        model=model,
        openai_api_base=OPENROUTER_BASE_URL,
        openai_api_key=get_openrouter_key(),
        **kwargs,
    )


def data_path(name: str) -> Path:
    """Resolve a filename under the repo-root data/ dir, regardless of CWD."""
    return REPO_ROOT / "data" / name
