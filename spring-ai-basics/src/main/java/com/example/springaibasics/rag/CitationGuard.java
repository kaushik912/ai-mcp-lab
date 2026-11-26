package com.example.springaibasics.rag;

import java.util.List;
import java.util.Set;

/**
 * Concept: validate model output in code. The model may "cite" a source it never
 * received (hallucinated citation). Keep only ids we actually retrieved.
 */
public final class CitationGuard {

	private CitationGuard() {}

	public static List<String> keepRetrieved(List<String> cited, Set<String> retrieved) {
		return cited.stream().filter(retrieved::contains).distinct().toList();
	}
}
