package com.example.resiliencellmfailover.service;

import com.example.resiliencellmfailover.dto.ChatResult;
import dev.langchain4j.model.chat.ChatModel;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ResilientAiService {

    private static final Logger log = LoggerFactory.getLogger(ResilientAiService.class);

    private final ChatModel geminiModel;
    private final ChatModel openRouterModel;

    public ResilientAiService(
            @Qualifier("geminiModel") ChatModel geminiModel,
            @Qualifier("openRouterModel") ChatModel openRouterModel) {
        this.geminiModel = geminiModel;
        this.openRouterModel = openRouterModel;
    }

    @CircuitBreaker(name = "geminiService", fallbackMethod = "fallbackToOpenRouter")
    public ChatResult generateResponse(String prompt) {
        log.info("Sending request to primary model [Gemini]...");
        String response = geminiModel.chat(prompt);
        return new ChatResult(response, ChatResult.Provider.GEMINI);
    }

    public ChatResult fallbackToOpenRouter(String prompt, Throwable exception) {
        log.warn(">>> Primary model [Gemini] failed! Reason: '{}'. Falling back to OpenRouter...",
                exception.getMessage());
        String response = openRouterModel.chat(prompt);
        return new ChatResult(response, ChatResult.Provider.OPENROUTER);
    }
}
