package com.example.a2a.llm;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** OpenAI (or OpenAI-compatible) models. Activated when a2a.llm.provider=openai. */
@Component
public class OpenAiModelFactory implements ChatModelFactory {

    private final String apiKey;
    private final String model;

    public OpenAiModelFactory(
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${a2a.llm.openai.model:gpt-4o-mini}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String provider() {
        return "openai";
    }

    @Override
    public ChatModel create(double temperature) {
        OpenAiApi api = OpenAiApi.builder().apiKey(apiKey).build();
        return OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build())
            .build();
    }
}