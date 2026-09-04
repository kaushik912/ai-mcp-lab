package com.example.a2a;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * A2A Agent #2 — generates an inspirational quote with an LLM (mistral).
 * Discovery card at: /quote/.well-known/agent-card.json
 * JSON-RPC endpoint : POST /quote
 */
@RestController
@RequestMapping("/quote")
public class QuoteAgentController {

    private final ChatClient chat;

    public QuoteAgentController(@Qualifier("quoteChatClient") ChatClient chat) {
        this.chat = chat;
    }

    @GetMapping("/.well-known/agent-card.json")
    public Map<String, Object> card() {
        return Map.of(
            "name", "Quote Agent",
            "description", "Generates an inspirational quote with an LLM.",
            "url", "http://localhost:8080/quote",
            "version", "1.0.0",
            "capabilities", Map.of("streaming", false),
            "defaultInputModes", List.of("text/plain"),
            "defaultOutputModes", List.of("text/plain"),
            "skills", List.of(Map.of(
                "id", "give_quote",
                "name", "Give a Quote",
                "description", "Returns an inspirational quote.",
                "tags", List.of("inspiration", "motivation", "quote")))
        );
    }

    @PostMapping
    public Map<String, Object> handle(@RequestBody Map<String, Object> req) {
        String userText = A2aSupport.userText(req);

        String quote = chat.prompt()
            .system("Respond with exactly ONE short inspirational quote and its author. "
                + "Format: \"quote\" - Author. No preamble.")
            .user(userText.isBlank() ? "Give me an inspiring quote." : userText)
            .call()
            .content()
            .trim();

        return A2aSupport.reply(req.get("id"), quote);
    }
}