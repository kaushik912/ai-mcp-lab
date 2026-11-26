package com.example.springaibasics.basics;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Concept: one ChatClient call. System prompt = our rules, user prompt = their data.
 * The API is stateless: this endpoint sends only the current message, so the model
 * cannot remember anything from a previous call. Compare with /memory/chat.
 */
@RestController
@RequestMapping("/basics")
class BasicsController {

	private static final String SYSTEM = "You are a concise assistant. Answer in at most two sentences.";

	private final ChatClient chatClient;

	BasicsController(ChatClient.Builder builder) {
		this.chatClient = builder.build();
	}

	record Reply(String answer, Integer promptTokens, Integer completionTokens) {}

	@PostMapping("/chat")
	Reply chat(@RequestBody String message) {
		ChatResponse response = chatClient.prompt()
				.system(SYSTEM)
				.user(message)
				.call()
				.chatResponse();          // full response, so we can read token usage

		Usage usage = response.getMetadata().getUsage();   // cost = tokens in + tokens out
		return new Reply(response.getResult().getOutput().getText(),
				usage.getPromptTokens(), usage.getCompletionTokens());
	}
}
