package com.example.springaibasics.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class CitationGuardTest {

	@Test
	void dropsSourcesThatWereNeverRetrieved() {
		List<String> cited = List.of("acme/refund-policy", "globex/secret-roadmap", "made-up/doc");
		Set<String> retrieved = Set.of("acme/refund-policy", "acme/plans");

		assertThat(CitationGuard.keepRetrieved(cited, retrieved)).containsExactly("acme/refund-policy");
	}
}
