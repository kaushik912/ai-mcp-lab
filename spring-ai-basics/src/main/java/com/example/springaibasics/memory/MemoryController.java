package com.example.springaibasics.memory;

import com.example.springaibasics.common.CurrentUser;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Concept: memory is an ADVISOR that resends earlier messages on every call.
 * - Window of 10 messages: bounded cost, oldest messages evicted.
 * - Conversation id comes from the logged-in user, not from the request.
 * - Storage here is the default in-memory repository: fine for a demo, wrong for
 *   several app instances (use the JDBC or Redis repository there).
 */
@RestController
@RequestMapping("/memory")
class MemoryController {

	private final ChatClient chatClient;
	private final CurrentUser currentUser;

	MemoryController(ChatClient.Builder builder, CurrentUser currentUser) {
		ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(10).build();
		this.chatClient = builder
				.defaultSystem("You are a friendly assistant. Keep answers short.")
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
				.build();
		this.currentUser = currentUser;
	}

	@PostMapping("/chat")
	String chat(@RequestBody String message) {
		String conversationId = currentUser.id() + ":default";   // derived on the server

		return chatClient.prompt()
				.user(message)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
				.call()
				.content();
	}
}
