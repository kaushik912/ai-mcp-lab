package com.example.randomquote;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RandomquoteApplication {

	public static void main(String[] args) {
		SpringApplication.run(RandomquoteApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider quoteToolProvider(QuoteTools quoteTools) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(quoteTools)
				.build();
	}

}