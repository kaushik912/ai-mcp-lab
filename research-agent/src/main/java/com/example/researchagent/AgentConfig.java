package com.example.researchagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the single Gemini-backed ChatClient: same "brain" (OpenAiChatModel
 * pointed at Google's OpenAI-compatible endpoint) but with tool calling
 * (MCP), retrieval (RAG over an in-memory vector store) and session memory
 * layered on as advisors — mirrors the article's ResearchAgent, minus the
 * Anthropic/Ollama options since this build is Gemini-only.
 */
@Configuration
public class AgentConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public ChatClient researchAgent(OpenAiChatModel geminiModel,
                                     ToolCallbackProvider mcpTools,
                                     VectorStore vectorStore,
                                     ChatMemory chatMemory) {
        return ChatClient.builder(geminiModel)
                .defaultToolCallbacks(mcpTools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore).build())
                .build();
    }
}
