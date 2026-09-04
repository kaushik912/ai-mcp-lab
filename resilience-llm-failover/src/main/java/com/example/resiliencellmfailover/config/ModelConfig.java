package com.example.resiliencellmfailover.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelConfig {

    @Bean("geminiModel")
    public ChatModel geminiModel(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model-name}") String modelName) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxRetries(1)
                .build();
    }

    @Bean("openRouterModel")
    public ChatModel openRouterModel(
            @Value("${openrouter.api-key}") String apiKey,
            @Value("${openrouter.base-url}") String baseUrl,
            @Value("${openrouter.model-name}") String modelName) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }
}
