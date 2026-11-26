package com.example.springaibasics.rag;

import com.example.springaibasics.common.CurrentUser;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rag")
class RagController {

	private final RagService ragService;
	private final CurrentUser currentUser;

	RagController(RagService ragService, CurrentUser currentUser) {
		this.ragService = ragService;
		this.currentUser = currentUser;
	}

	@PostMapping("/ask")
	RagService.Answer ask(@RequestBody String question) {
		return ragService.ask(currentUser.tenant(), question);   // tenant from the server, not the request
	}
}
