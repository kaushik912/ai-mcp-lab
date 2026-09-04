package com.example.newssearchtool;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Stub headline data — swap for a real news API (e.g. NewsAPI.org) by
 * replacing this class; NewsSearchTools's @Tool contract stays the same.
 */
@Service
public class NewsSearchService {

    private static final List<String> HEADLINES = List.of(
            "Java 25 ships with faster startup times via Project Leyden — TechDaily",
            "Spring AI 2.0 goes GA with first-class MCP support — DevWeekly",
            "Local LLMs close the gap with cloud models on coding benchmarks — AIToday",
            "Gemini tops multimodal reasoning leaderboards — TechDaily",
            "Vector databases see record adoption in enterprise RAG pipelines — DataWire",
            "Anthropic and Google both ship MCP-native agent tooling — DevWeekly"
    );

    public List<String> search(String topic) {
        if (topic == null || topic.isBlank()) {
            return HEADLINES;
        }
        String needle = topic.toLowerCase(Locale.ROOT);
        List<String> hits = HEADLINES.stream()
                .filter(h -> h.toLowerCase(Locale.ROOT).contains(needle))
                .toList();
        return hits.isEmpty() ? HEADLINES : hits;
    }
}
