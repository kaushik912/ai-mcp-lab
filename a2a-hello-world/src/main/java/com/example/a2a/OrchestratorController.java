package com.example.a2a;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * The host / orchestrator. An LLM decides which agent should handle the
 * request, then it delegates via an A2A "message/send" call.
 */
@RestController
public class OrchestratorController {

    private final ChatClient router;
    private final RestClient http;

    /** Known agents. In full A2A you'd fetch these cards at startup. */
    private static final Map<String, String> AGENTS = Map.of(
        "joke-agent",  "http://localhost:8080/joke",
        "quote-agent", "http://localhost:8080/quote"
    );

    public OrchestratorController(@Qualifier("routerChatClient") ChatClient router,
                                  RestClient http) {
        this.router = router;
        this.http = http;
    }

    @GetMapping("/ask")
    @SuppressWarnings("unchecked")
    public Map<String, Object> ask(@RequestParam String q) {
        // 1. ROUTE — the LLM picks the best agent for this request.
        String agentId = route(q);
        String agentUrl = AGENTS.get(agentId);

        // 2. DELEGATE — send an A2A message/send to the chosen agent.
        Map<String, Object> response = http.post()
            .uri(agentUrl)
            .body(A2aSupport.sendRequest(q))
            .retrieve()
            .body(Map.class);

        // 3. UNWRAP the agent's reply and return it.
        String answer = A2aSupport.answerText(response);
        return Map.of("routedTo", agentId, "answer", answer);
    }

    /**
     * LLM-based routing. Kept defensive: small local models sometimes add
     * chatter, so we look for keywords in the model's output rather than
     * trusting an exact id.
     */
    private String route(String query) {
        String decision = router.prompt()
            .system("""
                You are a router. Reply with ONLY one agent id that best handles
                the user's request. Output just the id, nothing else.
                Options:
                - joke-agent  (skills: tell jokes, humor)
                - quote-agent (skills: inspirational quotes, motivation)
                """)
            .user(query)
            .call()
            .content()
            .toLowerCase();

        return decision.contains("quote") ? "quote-agent" : "joke-agent";
    }
}