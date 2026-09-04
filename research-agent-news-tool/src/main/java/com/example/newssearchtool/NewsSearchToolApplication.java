package com.example.newssearchtool;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class NewsSearchToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewsSearchToolApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider newsToolProvider(NewsSearchTools newsSearchTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(newsSearchTools)
                .build();
    }
}
