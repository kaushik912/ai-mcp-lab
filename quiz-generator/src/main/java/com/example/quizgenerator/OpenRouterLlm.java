package com.example.quizgenerator;

import java.time.Duration;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** BYOK: a fresh Spring AI model is built per call with the caller's key. Nothing is cached or stored. */
@Component
public class OpenRouterLlm implements QuizLlm {

    private final String baseUrl;
    private final double temperature;
    private final Duration timeout;

    public OpenRouterLlm(
            @Value("${quiz.base-url}") String baseUrl,
            @Value("${quiz.temperature}") double temperature,
            @Value("${quiz.timeout}") Duration timeout) {
        this.baseUrl = baseUrl;
        this.temperature = temperature;
        this.timeout = timeout;
    }

    @Override
    public String complete(String apiKey, String model, String prompt) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .model(model)
                .temperature(temperature)
                .timeout(timeout)
                .maxRetries(0)
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder().options(options).build();
        ChatResponse response = chatModel.call(new Prompt(prompt));
        return response.getResult().getOutput().getText();
    }
}
