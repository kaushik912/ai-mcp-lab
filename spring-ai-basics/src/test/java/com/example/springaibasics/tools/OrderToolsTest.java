package com.example.springaibasics.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import com.example.springaibasics.tools.OrderTools.ToolFailure;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Tools are just methods, so their safety rules are tested without any model.
 * This is what a prompt-injected model would hit: the checks still hold.
 */
class OrderToolsTest {

	private final OrderTools tools = new OrderTools();
	private final ToolContext alice = new ToolContext(Map.of("userId", "alice"));

	@Test
	void customerSeesOwnOrder() {
		assertThat(tools.orderStatus("1001", alice)).contains("DELIVERED");
	}

	@Test
	void customerCannotSeeSomeoneElsesOrder() {
		assertThatThrownBy(() -> tools.orderStatus("1002", alice))   // bob's order
				.isInstanceOf(ToolFailure.class)
				.hasMessage("Order not found.");
	}

	@Test
	void refundAboveOrderTotalIsRefused() {
		assertThatThrownBy(() -> tools.requestRefund("1001", 5000, alice))
				.isInstanceOf(ToolFailure.class);
	}

	@Test
	void refundRequestIsIdempotentAndOnlyProposed() {
		String first = tools.requestRefund("1001", 20, alice);
		String second = tools.requestRefund("1001", 20, alice);

		assertThat(first).contains("REQ-1001").contains("pending human approval");
		assertThat(second).contains("REQ-1001");
	}
}
