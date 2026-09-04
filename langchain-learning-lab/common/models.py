"""Provider-agnostic model factory.

Example scripts should call `get_chat_model()` / `get_embeddings_model()` from
here instead of reaching into `common.env`'s provider-specific helpers
directly. That keeps example code free of any dependency on which provider is
actually in use - swap the default for every example by editing
`common.config.DEFAULT_PROVIDER`, or pass `provider=` to override per call.
"""
from common import env
from common.config import DEFAULT_PROVIDER

_CHAT_FACTORIES = {
    "gemini": env.get_gemini_chat,
    "openai": env.get_openai_chat,
    "openrouter": env.get_openrouter_chat,
}

_EMBEDDING_FACTORIES = {
    "gemini": env.get_gemini_embeddings,
    "openai": env.get_openai_embeddings,
    "openrouter": env.get_openrouter_embeddings,
}


def get_chat_model(provider: str | None = None, **kwargs):
    provider = provider or DEFAULT_PROVIDER
    try:
        factory = _CHAT_FACTORIES[provider]
    except KeyError:
        raise ValueError(f"Unknown chat provider {provider!r}. Choose from {list(_CHAT_FACTORIES)}.")
    return factory(**kwargs)


def get_embeddings_model(provider: str | None = None, **kwargs):
    provider = provider or DEFAULT_PROVIDER
    try:
        factory = _EMBEDDING_FACTORIES[provider]
    except KeyError:
        raise ValueError(f"Unknown embeddings provider {provider!r}. Choose from {list(_EMBEDDING_FACTORIES)}.")
    return factory(**kwargs)
