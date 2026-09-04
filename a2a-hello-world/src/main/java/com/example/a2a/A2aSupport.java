package com.example.a2a;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tiny helpers for building and parsing A2A (Agent2Agent) JSON-RPC messages.
 * A2A is just JSON-RPC 2.0 over HTTP, so these are plain Map manipulations.
 */
public final class A2aSupport {

    private A2aSupport() {
    }

    /** Extract the user's text from an incoming A2A "message/send" request. */
    @SuppressWarnings("unchecked")
    static String userText(Map<String, Object> req) {
        Map<String, Object> params = (Map<String, Object>) req.get("params");
        Map<String, Object> message = (Map<String, Object>) params.get("message");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) message.get("parts");
        Object text = parts.get(0).get("text");
        return text == null ? "" : text.toString();
    }

    /** Extract the agent's reply text from a JSON-RPC response body. */
    @SuppressWarnings("unchecked")
    static String answerText(Map<String, Object> response) {
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) result.get("parts");
        Object text = parts.get(0).get("text");
        return text == null ? "" : text.toString();
    }

    /** Wrap an agent's answer into a JSON-RPC A2A "message" result. */
    static Map<String, Object> reply(Object requestId, String text) {
        return Map.of(
            "jsonrpc", "2.0",
            "id", requestId,
            "result", Map.of(
                "kind", "message",
                "role", "agent",
                "messageId", UUID.randomUUID().toString(),
                "parts", List.of(Map.of("kind", "text", "text", text))
            )
        );
    }

    /** Build an outbound A2A "message/send" request body. */
    static Map<String, Object> sendRequest(String text) {
        return Map.of(
            "jsonrpc", "2.0",
            "id", UUID.randomUUID().toString(),
            "method", "message/send",
            "params", Map.of("message", Map.of(
                "role", "user",
                "messageId", UUID.randomUUID().toString(),
                "parts", List.of(Map.of("kind", "text", "text", text))
            ))
        );
    }
}