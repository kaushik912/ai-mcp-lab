package com.example.springaibasics.tools;

import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Concept: fence the loop. Every tool round is another billed model call, so cap it.
 */
@Configuration
class ToolsConfig {

	@Bean
	ToolCallingManager toolCallingManager() {
		return ToolCallingManager.builder()
				.maxTotalToolCalls(5)
				.build();
	}
}
