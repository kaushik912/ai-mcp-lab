package com.example.springaibasics.workflow;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** In production this would be a queue consumer, not an HTTP endpoint. HTTP keeps the demo simple. */
@RestController
@RequestMapping("/workflow")
class TriageController {

	private final TriageWorkflow workflow;

	TriageController(TriageWorkflow workflow) {
		this.workflow = workflow;
	}

	@PostMapping("/triage")
	TriageWorkflow.TriageResult triage(@RequestBody String ticket) {
		return workflow.triage(ticket);
	}
}
