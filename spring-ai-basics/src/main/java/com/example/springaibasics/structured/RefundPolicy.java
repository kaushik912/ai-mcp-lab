package com.example.springaibasics.structured;

/**
 * Concept: business rules live in CODE, not in the prompt. The model's answer can be
 * valid JSON and still be wrong or manipulated (prompt injection), so we re-check it.
 * Pure Java: unit-tested without any LLM.
 */
public final class RefundPolicy {

	public static final int AUTO_APPROVE_CAP = 50;

	public enum Action { APPROVE, REJECT, ESCALATE_TO_HUMAN }

	private RefundPolicy() {}

	public static Action decide(RefundDecision model, int orderTotal) {
		if (!model.eligible()) {
			return Action.REJECT;
		}
		if (model.amount() <= 0 || model.amount() > orderTotal || model.amount() > AUTO_APPROVE_CAP) {
			return Action.ESCALATE_TO_HUMAN;   // never trust the number blindly
		}
		return Action.APPROVE;
	}
}
