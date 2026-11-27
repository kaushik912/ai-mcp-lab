package com.aicommerce.storefront.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Ingest pipeline: Markdown -> one Document per section (with docType metadata) -> embeddings -> SimpleVectorStore.
 * SimpleVectorStore lives in memory, so documents are re-embedded on each start (~17 short chunks, a fraction of a cent).
 */
@Configuration
public class KnowledgeBaseConfig {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseConfig.class);
    static final String KEY_NOT_SET = "demo-key-not-set";

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /** Pure loading step, separate from embedding so it can be unit-tested without an API key. */
    public static List<Document> loadDocuments(Resource policy, Resource catalog) throws IOException {
        List<Document> docs = new ArrayList<>();
        docs.addAll(MarkdownSectionSplitter.split(policy.getContentAsString(StandardCharsets.UTF_8),
                "POLICY", policy.getFilename()));
        docs.addAll(MarkdownSectionSplitter.split(catalog.getContentAsString(StandardCharsets.UTF_8),
                "CATALOG", catalog.getFilename()));
        return docs;
    }

    @Bean
    ApplicationRunner ingestKnowledgeBase(VectorStore vectorStore,
            @Value("classpath:docs/return-and-warranty-policy.md") Resource policy,
            @Value("classpath:docs/product-guides.md") Resource catalog,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${app.rag.ingest-on-startup:true}") boolean enabled) {
        return args -> {
            if (!enabled || KEY_NOT_SET.equals(apiKey)) {
                log.warn("RAG ingestion skipped (OPENROUTER_API_KEY not set or app.rag.ingest-on-startup=false). "
                        + "Policy and catalog answers will not be grounded.");
                return;
            }
            List<Document> docs = loadDocuments(policy, catalog);
            try {
                vectorStore.add(docs);
                long policies = docs.stream().filter(d -> "POLICY".equals(d.getMetadata().get("docType"))).count();
                log.info("RAG knowledge base ready: {} chunks ({} POLICY, {} CATALOG)",
                        docs.size(), policies, docs.size() - policies);
            } catch (RuntimeException e) {
                // Keep the storefront up even if the embedding API is down; chat degrades to ungrounded.
                log.error("RAG ingestion failed, continuing without knowledge base: {}", e.getMessage());
            }
        };
    }
}
