package com.aicommerce.storefront.trace;

import java.util.Optional;

/**
 * Binds the current turn's AgentTrace to the request thread. The synchronous ChatClient runs the advisor
 * chain and the tool loop on the caller's thread, so tool callbacks can find the trace without plumbing.
 */
public final class TraceRecorder {

    private static final ThreadLocal<AgentTrace> CURRENT = new ThreadLocal<>();

    private TraceRecorder() {}

    static void bind(AgentTrace trace) { CURRENT.set(trace); }
    static void unbind() { CURRENT.remove(); }

    public static Optional<AgentTrace> current() {
        return Optional.ofNullable(CURRENT.get());
    }
}
