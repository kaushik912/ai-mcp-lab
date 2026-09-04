package com.example.a2a;

import com.example.a2a.llm.ChatModelFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Selects an LLM provider via the {@code a2a.llm.provider} property (default
 * "ollama") and builds one ChatClient per role. Spring injects every
 * {@link ChatModelFactory} on the classpath; we pick the one whose
 * {@link ChatModelFactory#provider()} matches the configured value.
 *
 * Swapping providers is now pure configuration — no code changes here.
 */
@Configuration
public class AiConfig {

    private final ChatModelFactory factory;

    public AiConfig(List<ChatModelFactory> factories,
                    @Value("${a2a.llm.provider:ollama}") String provider) {
        this.factory = factories.stream()
            .filter(f -> f.provider().equalsIgnoreCase(provider))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unknown a2a.llm.provider='" + provider + "'. Available: "
                    + factories.stream().map(ChatModelFactory::provider).toList()));
    }

    @Bean("jokeChatClient")
    ChatClient jokeChatClient() {
        return ChatClient.create(factory.create(0.9));   // creative -> varied jokes
    }

    @Bean("quoteChatClient")
    ChatClient quoteChatClient() {
        return ChatClient.create(factory.create(0.8));
    }

    @Bean("routerChatClient")
    ChatClient routerChatClient() {
        return ChatClient.create(factory.create(0.0));   // deterministic -> routing
    }
}