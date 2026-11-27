package com.example.springaibasics.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Eval layer 2: RETRIEVAL only. Embeddings + vector search, no chat model.
 * If this fails, the answer was doomed before the LLM saw it (wrong or missing context).
 * If this passes but the judge fails, the problem is generation (prompt or model).
 *
 * Retrieval with a fixed embedding model is near-deterministic, so we expect every case to pass.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".+")
class RagRetrievalEvalTest {

	@Autowired
	RagService rag;

	record Case(String tenant, String question, String expectedSource) {}

	private static final List<Case> CASES = List.of(
			new Case("acme", "How many days do I have to ask for a refund?", "acme/refund-policy"),
			new Case("acme", "how do i reset my pasword", "acme/password-reset"),
			new Case("acme", "What does the Pro plan cost?", "acme/plans"),
			new Case("globex", "Can I get my money back?", "globex/refund-policy"));

	@Test
	void expectedSourceIsInTopK() {
		for (Case c : CASES) {
			List<String> sources = rag.retrieve(c.tenant(), c.question()).stream()
					.map(d -> String.valueOf(d.getMetadata().get("source")))
					.toList();

			assertThat(sources).as("retrieved for '%s'", c.question()).contains(c.expectedSource());
		}
	}

	@Test
	void retrievalNeverCrossesTenants() {
		List<Document> hits = rag.retrieve("acme", "What is the Globex Q3 launch codename?");

		assertThat(hits).allMatch(d -> "acme".equals(d.getMetadata().get("tenantId")));
	}
}
