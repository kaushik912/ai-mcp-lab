package com.example.a2a.llm;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Anthropic (Claude) models. Activated when a2a.llm.provider=anthropic. */
@Component
public class AnthropicModelFactory implements ChatModelFactory {

    private final String apiKey;
    private final String model;

    public AnthropicModelFactory(
            @Value("${spring.ai.anthropic.api-key:}") String apiKey,
            @Value("${a2a.llm.anthropic.model:claude-sonnet-5}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String provider() {
        return "anthropic";
    }

    @Override
    public ChatModel create(double temperature) {
        AnthropicApi api = AnthropicApi.builder().apiKey(apiKey).build();
        return AnthropicChatModel.builder()
            .anthropicApi(api)
            .defaultOptions(AnthropicChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build())
            .build();
    }
}