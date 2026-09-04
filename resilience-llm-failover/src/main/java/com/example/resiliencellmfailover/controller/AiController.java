package com.example.resiliencellmfailover.controller;

import com.example.resiliencellmfailover.dto.ChatRequest;
import com.example.resiliencellmfailover.dto.ChatResponse;
import com.example.resiliencellmfailover.service.ResilientAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "AI Chat", description = "Resilient chat endpoint that fails over from Gemini to OpenRouter on rate limits")
public class AiController {

    private final ResilientAiService aiService;

    public AiController(ResilientAiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    @Operation(
            summary = "Generate a chat response",
            description = "Routes to Gemini by default; automatically falls back to OpenRouter when Gemini is rate-limited"
    )
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return ChatResponse.from(aiService.generateResponse(request.prompt()));
    }
}
