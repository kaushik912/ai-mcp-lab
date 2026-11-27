package com.example.a2a.llm;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * OpenAI-compatible models (OpenRouter by default). Activated when a2a.llm.provider=openai.
 * Spring AI 1.0 appends /v1/chat/completions itself, so base-url must NOT end in /v1.
 */
@Component
public class OpenAiModelFactory implements ChatModelFactory {

    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public OpenAiModelFactory(
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${a2a.llm.openai.base-url:https://openrouter.ai/api}") String baseUrl,
            @Value("${a2a.llm.openai.model:deepseek/deepseek-v4.1-flash}") String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    @Override
    public String provider() {
        return "openai";
    }

    @Override
    public ChatModel create(double temperature) {
        OpenAiApi api = OpenAiApi.builder().baseUrl(baseUrl).apiKey(apiKey).build();
        return OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build())
            .build();
    }
}