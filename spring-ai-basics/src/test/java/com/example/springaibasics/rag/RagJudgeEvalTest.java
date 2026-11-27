package com.example.springaibasics.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.Evaluator;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Eval layer 3: LLM-as-judge. A second model grades the RAG answers.
 *
 *  - FactCheckingEvaluator: is the answer SUPPORTED by the retrieved documents? (catches hallucination)
 *  - RelevancyEvaluator:    does the answer actually address the question, in line with the context?
 *
 * The judge is a DIFFERENT model from production, so the model isn't grading its own homework.
 * Both the answer and the judge are non-deterministic, so we assert a pass RATE, not 100%.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".+")
class RagJudgeEvalTest {

	static final String JUDGE_MODEL = "qwen/qwen3.7-flash";

	// Both evaluators pass only if the reply is exactly "yes" (case-insensitive), so ask for one bare word.
	private static final String FACT_CHECK_PROMPT = """
			Evaluate whether or not the following claim is supported by the provided document.
			Reply with exactly one lowercase word, "yes" or "no", with no punctuation.

			Document:
			{document}

			Claim:
			{claim}
			""";

	@Autowired
	RagService rag;

	@Autowired
	ChatClient.Builder builder;   // prototype-scoped: we get our own copy, prod's client is untouched

	Evaluator factCheck;
	Evaluator relevancy;

	@BeforeEach
	void judge() {
		// Same OpenRouter connection, different model: override it in this builder's default options.
		ChatClient.Builder judge = builder.defaultOptions(OpenAiChatOptions.builder()
				.model(JUDGE_MODEL)
				.temperature(0.0));
		factCheck = FactCheckingEvaluator.builder(judge).evaluationPrompt(FACT_CHECK_PROMPT).build();
		relevancy = RelevancyEvaluator.builder().chatClientBuilder(judge).build();
	}

	private static final List<String> QUESTIONS = List.of(
			"How many days do I have to ask for a refund?",
			"how do i reset my pasword",
			"What does the Pro plan cost?");

	@Test
	void judgePassRate() {
		int passed = 0;
		for (String q : QUESTIONS) {
			List<Document> context = rag.retrieve("acme", q);       // what the RAG model was shown
			String answer = rag.ask("acme", q).answer();
			EvaluationRequest req = new EvaluationRequest(q, context, answer);

			boolean grounded = factCheck.evaluate(req).isPass();
			boolean relevant = relevancy.evaluate(req).isPass();
			System.out.printf("[judge] %-45s grounded=%-5s relevant=%-5s answer=%s%n", q, grounded, relevant, answer);
			if (grounded && relevant) {
				passed++;
			}
		}

		assertThat(passed).as("passed %d of %d", passed, QUESTIONS.size()).isGreaterThanOrEqualTo(2);
	}

	/** Sanity check on the judge itself: a made-up answer must be flagged. */
	@Test
	void judgeCatchesAHallucination() {
		String q = "How many days do I have to ask for a refund?";
		EvaluationRequest req = new EvaluationRequest(q, rag.retrieve("acme", q),
				"You have 90 days, and refunds are paid in cash at any store.");

		assertThat(factCheck.evaluate(req).isPass()).isFalse();
	}
}
