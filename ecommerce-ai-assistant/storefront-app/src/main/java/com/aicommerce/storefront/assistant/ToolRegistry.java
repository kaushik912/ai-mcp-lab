package com.aicommerce.storefront.assistant;

import com.aicommerce.storefront.tools.OrderAndReturnTools;
import com.aicommerce.storefront.tools.ProductCatalogTools;
import com.aicommerce.storefront.trace.AgentTrace.ToolKind;
import com.aicommerce.storefront.trace.TracingToolCallback;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Assembles the tools offered to the model for one turn, each wrapped for tracing.
 * Local tools are built once. MCP tools are fetched per turn so the storefront keeps working
 * (without carrier tools) when carrier-mcp-server is down, and picks them up once it is back.
 */
@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);
    static final String CARRIER_SERVER = "carrier-mcp-server";

    private final List<ToolCallback> local;
    private final ObjectProvider<SyncMcpToolCallbackProvider> mcp;

    public ToolRegistry(ProductCatalogTools catalog, OrderAndReturnTools orders,
                        ObjectProvider<SyncMcpToolCallbackProvider> mcp) {
        this.local = Arrays.stream(ToolCallbacks.from(catalog, orders))
                .map(cb -> (ToolCallback) new TracingToolCallback(cb, ToolKind.LOCAL, "storefront-app"))
                .toList();
        this.mcp = mcp;
    }

    /** @param note set when MCP tools could not be loaded; shown in the trace */
    public record TurnTools(List<ToolCallback> callbacks, String note) {}

    public TurnTools forTurn() {
        List<ToolCallback> all = new ArrayList<>(local);
        SyncMcpToolCallbackProvider provider = mcp.getIfAvailable();
        if (provider == null) {
            return new TurnTools(all, "MCP client disabled: carrier tools not offered.");
        }
        try {
            for (ToolCallback cb : provider.getToolCallbacks()) {
                all.add(new TracingToolCallback(cb, ToolKind.MCP, CARRIER_SERVER));
            }
            return new TurnTools(all, null);
        } catch (RuntimeException e) {
            log.warn("Carrier MCP server unavailable, continuing with local tools only: {}", e.getMessage());
            return new TurnTools(all, "Carrier MCP server unavailable: carrier tools not offered this turn.");
        }
    }

    public List<ToolCallback> localTools() {
        return local;
    }
}
