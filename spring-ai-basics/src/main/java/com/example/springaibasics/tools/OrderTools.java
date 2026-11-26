package com.example.springaibasics.tools;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Concept: tools are methods the model can ASK to call. Our code runs them.
 * Rules shown here:
 *  - identity (userId) comes from ToolContext, set by the server; the model never sees or sets it
 *  - the service checks ownership (alice cannot read bob's order)
 *  - write tool only PROPOSES (pending human approval), and is idempotent per order
 *  - errors are short and safe: the exception message goes back to the model
 *  - no "run any SQL" tool: only narrow operations
 */
@Component
public class OrderTools {

	public record Order(String orderNo, String owner, int total, String status) {}

	private final Map<String, Order> orders = Map.of(
			"1001", new Order("1001", "alice", 20, "DELIVERED"),
			"1002", new Order("1002", "bob", 80, "SHIPPED"),
			"2001", new Order("2001", "carol", 35, "DELIVERED"));

	/** orderNo -> request id. Makes requestRefund idempotent: asking twice gives the same request. */
	private final Map<String, String> pendingRefunds = new ConcurrentHashMap<>();

	@Tool(description = "Get the status and total of one of the current customer's orders")
	public String orderStatus(
			@ToolParam(description = "Order number, for example 1001") String orderNo,
			ToolContext ctx) {
		Order order = ownedOrder(orderNo, userId(ctx));
		return "Order " + order.orderNo() + " is " + order.status() + ", total $" + order.total();
	}

	@Tool(description = "Request a refund for one of the current customer's orders. "
			+ "This creates a request for a human to approve; it does not pay anything.")
	public String requestRefund(
			@ToolParam(description = "Order number") String orderNo,
			@ToolParam(description = "Refund amount in whole dollars") int amount,
			ToolContext ctx) {
		Order order = ownedOrder(orderNo, userId(ctx));
		if (amount <= 0 || amount > order.total()) {
			throw new ToolFailure("Refund amount must be between 1 and the order total.");
		}
		String requestId = pendingRefunds.computeIfAbsent(orderNo, k -> "REQ-" + k);
		return "Refund request " + requestId + " for $" + amount + " is pending human approval.";
	}

	private Order ownedOrder(String orderNo, String userId) {
		Order order = orders.get(orderNo);
		if (order == null || !order.owner().equals(userId)) {
			// Same message for "missing" and "not yours": don't reveal other customers' orders.
			throw new ToolFailure("Order not found.");
		}
		return order;
	}

	private static String userId(ToolContext ctx) {
		return (String) ctx.getContext().get("userId");
	}

	/** Short, safe message. RuntimeException messages are sent back to the model by default. */
	public static class ToolFailure extends RuntimeException {
		public ToolFailure(String message) {
			super(message);
		}
	}
}
