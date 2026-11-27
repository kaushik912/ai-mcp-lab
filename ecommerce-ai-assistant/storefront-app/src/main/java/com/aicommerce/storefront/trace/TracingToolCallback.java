package com.aicommerce.storefront.trace;

import com.aicommerce.storefront.trace.AgentTrace.ToolCall;
import com.aicommerce.storefront.trace.AgentTrace.ToolKind;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * Decorator around any ToolCallback (local @Tool or remote MCP) that records name, sanitized args,
 * latency, success and outcome into the current AgentTrace.
 *
 * For MCP tools the server-side ToolContext (which holds the authenticated user id) is deliberately
 * NOT forwarded: the external carrier must never receive the customer's identity.
 */
public class TracingToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(TracingToolCallback.class);
    private static final Pattern STATUS = Pattern.compile("\"status\"\\s*:\\s*\"([A-Z_]+)\"");
    private static final Pattern SECRET = Pattern.compile(
            "(\"(?:[^\"]*(?:password|token|secret|apiKey|api_key)[^\"]*)\"\\s*:\\s*)\"[^\"]*\"", Pattern.CASE_INSENSITIVE);

    private static final java.util.Set<String> DECISION_CODES = java.util.Arrays
            .stream(com.aicommerce.storefront.returns.DecisionStatus.values()).map(Enum::name)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private final ToolCallback delegate;
    private final ToolKind kind;
    private final String server;

    public TracingToolCallback(ToolCallback delegate, ToolKind kind, String server) {
        this.delegate = delegate;
        this.kind = kind;
        this.server = server;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String name = getToolDefinition().name();
        long t0 = System.nanoTime();
        try {
            String result = kind == ToolKind.MCP
                    ? delegate.call(toolInput)                 // identity stays inside the storefront
                    : delegate.call(toolInput, toolContext);
            record(name, toolInput, t0, true, outcome(result), result);
            return result;
        } catch (RuntimeException e) {
            record(name, toolInput, t0, false, "ERROR", e.getMessage());
            throw e;
        }
    }

    private void record(String name, String input, long t0, boolean ok, String outcome, String result) {
        long ms = (System.nanoTime() - t0) / 1_000_000;
        log.info("[TOOL {}] {} {}ms outcome={} args={}", kind, name, ms, outcome, sanitize(input));
        TraceRecorder.current().ifPresent(t -> t.addTool(new ToolCall(name, kind, server, sanitize(input), ms, ok,
                outcome, truncate(result, 400))));
    }

    static String sanitize(String json) {
        return json == null ? "" : truncate(SECRET.matcher(json).replaceAll("$1\"***\""), 300);
    }

    /** A decision code (APPROVED, REJECTED_..., VOUCHER_ISSUED...) if the tool returned one, else OK. */
    private static String outcome(String result) {
        if (result == null) {
            return "OK";
        }
        Matcher m = STATUS.matcher(result);
        while (m.find()) {
            if (DECISION_CODES.contains(m.group(1))) {
                return m.group(1);
            }
        }
        return "OK";
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
