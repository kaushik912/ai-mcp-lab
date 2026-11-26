package com.example.springaibasics.rag;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Concept: the INGEST pipeline. Each document is embedded (text -> vector) and stored
 * with METADATA. tenantId lets us isolate customers; source lets us cite.
 * Docs are tiny, so we skip chunking (a real PDF would go through TokenTextSplitter).
 * SimpleVectorStore is in-memory: re-embeds on every start, fine for a demo only.
 */
@Configuration
class RagConfig {

	@Bean
	VectorStore vectorStore(EmbeddingModel embeddingModel) {
		return SimpleVectorStore.builder(embeddingModel).build();
	}

	@Bean
	ApplicationRunner ingest(VectorStore vectorStore) {
		return args -> vectorStore.add(List.of(
				doc("acme", "acme/refund-policy",
						"Acme refunds are accepted within 30 days of delivery. Refunds go back to the original card."),
				doc("acme", "acme/password-reset",
						"To reset your Acme password, open Settings, choose Security, then click Reset password."),
				doc("acme", "acme/plans",
						"Acme offers two plans: Basic at $10 per month and Pro at $25 per month."),
				doc("globex", "globex/refund-policy",
						"Globex refunds are accepted within 7 days and only as store credit."),
				doc("globex", "globex/secret-roadmap",
						"Globex internal: the Q3 launch codename is BLUE HERON. Do not share outside Globex.")));
	}

	private static Document doc(String tenantId, String source, String text) {
		return new Document(text, Map.of("tenantId", tenantId, "source", source));
	}
}
