package com.example.springaibasics.workflow;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Concept: a WORKFLOW, not an agent. The steps are fixed, so code chooses them.
 *   1. LLM classifies (typed, limited to enums)
 *   2. LLM summarises
 *   3. CODE routes (a plain switch)
 * Exactly two model calls per ticket, no tools, no memory, predictable cost.
 */
@Service
public class TriageWorkflow {

	public enum Category { BILLING, TECHNICAL, ACCOUNT, OTHER }

	public enum Priority { P1, P2, P3 }

	public record Classification(Category category, Priority priority) {}

	public record TriageResult(Classification classification, String summary, String queue) {}

	private final ChatClient chatClient;

	TriageWorkflow(ChatClient.Builder builder) {
		this.chatClient = builder.build();
	}

	public TriageResult triage(String ticket) {
		Classification c = chatClient.prompt()
				.system("Classify the support ticket. Pick exactly one category and one priority. "
						+ "P1 means the customer cannot use the product at all.")
				.user(ticket)
				.call()
				.entity(Classification.class);

		String summary = chatClient.prompt()
				.system("Summarise the support ticket in one sentence for a support agent. Omit personal data.")
				.user(ticket)
				.call()
				.content();

		return new TriageResult(c, summary, route(c.category()));
	}

	/** Step 3 is plain Java: deterministic and unit-testable. */
	public static String route(Category category) {
		return switch (category) {
			case BILLING -> "billing-queue";
			case TECHNICAL -> "tech-queue";
			case ACCOUNT -> "accounts-queue";
			case OTHER -> "general-queue";
		};
	}
}
