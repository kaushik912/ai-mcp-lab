package com.example.a2a.llm;

import org.springframework.ai.chat.model.ChatModel;

/**
 * Factory abstraction over LLM providers. Each implementation knows how to
 * build a provider-specific {@link ChatModel}. The active one is selected at
 * runtime by the {@code a2a.llm.provider} property (see AiConfig).
 */
public interface ChatModelFactory {

    /** Provider id this factory handles, e.g. "ollama", "openai", "anthropic". */
    String provider();

    /** Build a ChatModel using this provider's configured model + given temperature. */
    ChatModel create(double temperature);
}