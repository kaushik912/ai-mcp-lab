package com.aicommerce.storefront.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Everything that happened "under the hood" for one chat turn. Shown in the X-Ray drawer (Phase 5).
 * Thread-confined while the turn runs (see TraceRecorder), then read-only.
 */
public class AgentTrace {

    public enum ToolKind { LOCAL, MCP }

    public record RagHit(String docType, String sectionId, String title, Double score) {}

    /**
     * @param outcome the "status" field of the tool result, if any (e.g. APPROVED, REJECTED_OUTSIDE_WINDOW)
     */
    public record ToolCall(String name, ToolKind kind, String server, String arguments, long durationMs,
                           boolean success, String outcome, String resultPreview) {}

    private final Long injectedUserId;
    private final String injectedUsername;
    private final List<RagHit> ragHits = new ArrayList<>();
    private final List<ToolCall> toolCalls = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private long totalMs;

    public AgentTrace(Long injectedUserId, String injectedUsername) {
        this.injectedUserId = injectedUserId;
        this.injectedUsername = injectedUsername;
    }

    void addTool(ToolCall call) { toolCalls.add(call); }
    void addRag(RagHit hit) { ragHits.add(hit); }
    public void addNote(String note) { notes.add(note); }
    void setTotalMs(long ms) { this.totalMs = ms; }

    public Long getInjectedUserId() { return injectedUserId; }
    public String getInjectedUsername() { return injectedUsername; }
    public List<RagHit> getRagHits() { return Collections.unmodifiableList(ragHits); }
    public List<ToolCall> getToolCalls() { return Collections.unmodifiableList(toolCalls); }
    public List<String> getNotes() { return Collections.unmodifiableList(notes); }
    public long getTotalMs() { return totalMs; }
}
