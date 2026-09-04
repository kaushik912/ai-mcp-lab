package com.example.a2a;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * A2A Agent #1 — generates a joke with an LLM (llama3.2).
 * Discovery card at: /joke/.well-known/agent-card.json
 * JSON-RPC endpoint : POST /joke
 */
@RestController
@RequestMapping("/joke")
public class JokeAgentController {

    private final ChatClient chat;

    public JokeAgentController(@Qualifier("jokeChatClient") ChatClient chat) {
        this.chat = chat;
    }

    @GetMapping("/.well-known/agent-card.json")
    public Map<String, Object> card() {
        return Map.of(
            "name", "Joke Agent",
            "description", "Generates a random joke with an LLM.",
            "url", "http://localhost:8080/joke",
            "version", "1.0.0",
            "capabilities", Map.of("streaming", false),
            "defaultInputModes", List.of("text/plain"),
            "defaultOutputModes", List.of("text/plain"),
            "skills", List.of(Map.of(
                "id", "tell_joke",
                "name", "Tell a Joke",
                "description", "Returns a fresh joke.",
                "tags", List.of("fun", "humor", "joke")))
        );
    }

    @PostMapping
    public Map<String, Object> handle(@RequestBody Map<String, Object> req) {
        String userText = A2aSupport.userText(req);

        // A fixed prompt + fixed seed makes the model return the same joke every
        // time. For generic asks, seed a random topic so each call differs.
        String prompt;
        String lower = userText.toLowerCase();
        boolean generic = userText.isBlank()
            || lower.contains("laugh") || lower.contains("random")
            || lower.matches(".*\\btell me a joke\\b.*") || lower.equals("joke");
        if (generic) {
            String topic = TOPICS.get(random.nextInt(TOPICS.size()));
            prompt = "Tell me a short, original joke about " + topic + ".";
        } else {
            prompt = userText;   // respect a specific request, e.g. "joke about cats"
        }

        String joke = chat.prompt()
            .system("You are a comedian. Respond with exactly ONE short, original joke. No preamble, no explanation.")
            .user(prompt)
            .call()
            .content()
            .trim();

        return A2aSupport.reply(req.get("id"), joke);
    }

    private static final List<String> TOPICS = List.of(
        "cats", "programming", "coffee", "Mondays", "robots", "pizza",
        "the ocean", "music", "space", "dad life", "cars", "weather");

    private final java.util.Random random = new java.util.Random();
}