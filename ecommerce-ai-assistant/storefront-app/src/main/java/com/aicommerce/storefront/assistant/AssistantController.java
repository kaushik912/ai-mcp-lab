package com.aicommerce.storefront.assistant;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JSON chat endpoint (the Phase 5 UI will call it). The HTTP session id keys chat memory, so keep the cookie:
 *   curl -u alex:demo -c jar -b jar -H 'Content-Type: text/plain' -d 'Return my jacket from ORD-1003' localhost:8080/api/assistant/ask
 * Note: there is no user field in the request. Identity is the authenticated principal only.
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistant;

    public AssistantController(AssistantService assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/ask")
    public AssistantService.AssistantReply ask(@RequestBody String question, HttpSession session) {
        return assistant.ask(session.getId(), question);
    }

    @DeleteMapping("/conversation")
    public void reset(HttpSession session) {
        assistant.clearConversation(session.getId());
    }
}
