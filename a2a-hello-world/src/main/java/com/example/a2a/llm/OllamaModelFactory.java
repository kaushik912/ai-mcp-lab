package com.example.a2a.llm;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Local models via Ollama. This is the default provider. */
@Component
public class OllamaModelFactory implements ChatModelFactory {

    private final String baseUrl;
    private final String model;

    public OllamaModelFactory(
            @Value("${a2a.llm.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${a2a.llm.ollama.model:mistral}") String model) {
        this.baseUrl = baseUrl;
        this.model = model;
    }

    @Override
    public String provider() {
        return "ollama";
    }

    @Override
    public ChatModel create(double temperature) {
        OllamaApi api = OllamaApi.builder().baseUrl(baseUrl).build();
        return OllamaChatModel.builder()
            .ollamaApi(api)
            .defaultOptions(OllamaOptions.builder()
                .model(model)
                .temperature(temperature)
                .build())
            .build();
    }
}