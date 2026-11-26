package com.example.springaibasics.tools;

import java.util.Map;

import com.example.springaibasics.common.CurrentUser;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Concept: the tool loop. The model may request orderStatus / requestRefund; Spring AI
 * runs our methods and sends results back until the model answers.
 * The loop is ToolCallingAdvisor (a recursive advisor). Spring Boot auto-registers it on
 * the injected ChatClient.Builder, so we only pass .tools(...). Opt out per call with
 * AdvisorParams.toolCallingAdvisorAutoRegister(false) to drive the loop yourself.
 * The number of tool calls per request is capped in ToolsConfig.
 */
@RestController
@RequestMapping("/tools")
class SupportController {

	private static final String SYSTEM = """
			You are a support assistant for an online shop. Use the tools to look up orders
			and to request refunds. Be brief.
			""";

	private final ChatClient chatClient;
	private final OrderTools orderTools;
	private final CurrentUser currentUser;

	SupportController(ChatClient.Builder builder, OrderTools orderTools, CurrentUser currentUser) {
		this.chatClient = builder.build();
		this.orderTools = orderTools;
		this.currentUser = currentUser;
	}

	@PostMapping("/support")
	String support(@RequestBody String message) {
		return chatClient.prompt()
				.system(SYSTEM)
				.user(message)
				.tools(orderTools)
				// Not sent to the model. Handed to the tool methods by Spring AI.
				.toolContext(Map.of("userId", currentUser.id(), "tenantId", currentUser.tenant()))
				.call()
				.content();
	}
}
