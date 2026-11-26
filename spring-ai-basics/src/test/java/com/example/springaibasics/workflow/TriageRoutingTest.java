package com.example.springaibasics.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.springaibasics.workflow.TriageWorkflow.Category;
import org.junit.jupiter.api.Test;

/** In a workflow the decision step is plain code, so it is tested like plain code. */
class TriageRoutingTest {

	@Test
	void eachCategoryHasAQueue() {
		assertThat(TriageWorkflow.route(Category.BILLING)).isEqualTo("billing-queue");
		assertThat(TriageWorkflow.route(Category.TECHNICAL)).isEqualTo("tech-queue");
		assertThat(TriageWorkflow.route(Category.ACCOUNT)).isEqualTo("accounts-queue");
		assertThat(TriageWorkflow.route(Category.OTHER)).isEqualTo("general-queue");
	}
}
