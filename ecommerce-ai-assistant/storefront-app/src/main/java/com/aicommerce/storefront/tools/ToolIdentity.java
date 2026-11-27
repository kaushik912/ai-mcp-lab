package com.aicommerce.storefront.tools;

import org.springframework.ai.chat.model.ToolContext;

/**
 * Reads the server-injected user id from ToolContext. ToolContext is filled by AssistantService from the
 * Spring Security principal; it is never part of a tool's JSON schema, so the model can neither see nor set it.
 */
public final class ToolIdentity {

    public static final String USER_ID = "authenticatedUserId";

    private ToolIdentity() {}

    public static Long userId(ToolContext ctx) {
        Object v = ctx == null ? null : ctx.getContext().get(USER_ID);
        if (v instanceof Long id) {
            return id;
        }
        // Fail closed: no identity, no data.
        throw new IllegalStateException("No authenticated user in tool context");
    }
}
