package com.example.mcpclient;

import java.util.Arrays;
import java.util.List;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The "host" side of MCP: it owns the LLMs and lets them decide whether to call
 * the tools discovered from the remote quote MCP server.
 *
 * Concrete model beans are injected by type (OllamaChatModel,
 * AnthropicChatModel, OpenAiChatModel) — that sidesteps the ChatClient.Builder
 * ambiguity you'd hit if you injected the builder while multiple chat models
 * are on the classpath. OpenRouter rides OpenAiChatModel via its
 * OpenAI-compatible endpoint (see application.properties spring.ai.openai.*).
 *
 * `mcpTools` is the ToolCallbackProvider auto-created by the MCP client
 * autoconfig: at startup it connected to the server(s) in application.properties,
 * ran tools/list, and wrapped the results. We never name "randomQuote" here —
 * it is discovered.
 */
@RestController
public class ChatController {

    private final ChatClient ollamaClient;
    private final ChatClient anthropicClient;
    private final ChatClient openRouterClient;
    private final ToolCallbackProvider mcpTools;

    public ChatController(OllamaChatModel ollamaModel,
                          AnthropicChatModel anthropicModel,
                          OpenAiChatModel openAiModel,
                          ToolCallbackProvider mcpTools) {
        this.mcpTools = mcpTools;
        // same MCP tools handed to every model — only the brain differs
        this.ollamaClient = ChatClient.builder(ollamaModel)
                .defaultToolCallbacks(mcpTools)
                .build();
        this.anthropicClient = ChatClient.builder(anthropicModel)
                .defaultToolCallbacks(mcpTools)
                .build();
        this.openRouterClient = ChatClient.builder(openAiModel)
                .defaultToolCallbacks(mcpTools)
                .build();
    }

    @GetMapping("/chat/ollama")
    public String ollama(@RequestParam String q) {
        return ollamaClient.prompt().user(q).call().content();
    }

    @GetMapping("/chat/anthropic")
    public String anthropic(@RequestParam String q) {
        return anthropicClient.prompt().user(q).call().content();
    }

    @GetMapping("/chat/openrouter")
    public String openRouter(@RequestParam String q) {
        return openRouterClient.prompt().user(q).call().content();
    }

    /** Debug: what tools were discovered from the MCP server(s) at startup? */
    @GetMapping("/tools")
    public List<String> tools() {
        return Arrays.stream(mcpTools.getToolCallbacks())
                .map(tc -> tc.getToolDefinition().name() + " : " + tc.getToolDefinition().description())
                .toList();
    }
}