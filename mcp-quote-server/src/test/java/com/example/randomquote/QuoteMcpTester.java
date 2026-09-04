package com.example.randomquote;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

import java.time.Duration;
import java.util.Map;

/**
 * Scratch MCP client — Option A, no LLM.
 *
 * PREREQ: the app must already be running (e.g. `./mvnw spring-boot:run`) — the
 * same instance Claude talks to on port 8080. This just plays the role of an MCP
 * client and drives the tool by name.
 *
 * Run it: right-click -> Run 'QuoteMcpTester.main()' in your IDE.
 *
 * Tweak the three knobs below to experiment with different tools / args.
 */
public class QuoteMcpTester {

    // --- knobs to change while experimenting -------------------------------
    private static final String SERVER_BASE_URL = "http://localhost:8080"; // "/sse" is the default endpoint
    private static final String TOOL_NAME       = "randomQuote";
    private static final Map<String, Object> ARGS = Map.of(); // e.g. Map.of("category", "tech")
    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        var transport = HttpClientSseClientTransport
                .builder(SERVER_BASE_URL)
                .build();

        try (McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .build()) {

            client.initialize(); // handshake

            // tools/list — see what the server advertises
            ListToolsResult tools = client.listTools();
            System.out.println("Available tools:");
            tools.tools().forEach(t -> System.out.println("  - " + t.name() + " : " + t.description()));

            // tools/call — invoke the tool
            CallToolResult result = client.callTool(new CallToolRequest(TOOL_NAME, ARGS));
            System.out.println("\nCall '" + TOOL_NAME + "' " + ARGS);
            System.out.println("  isError = " + result.isError());
            System.out.println("  content = " + result.content());
        }
    }
}