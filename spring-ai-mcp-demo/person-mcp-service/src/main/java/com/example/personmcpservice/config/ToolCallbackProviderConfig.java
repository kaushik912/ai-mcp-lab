package com.example.personmcpservice.config;

import com.example.personmcpservice.tool.PersonToolService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolCallbackProviderConfig {

    @Bean
    public ToolCallbackProvider personToolCallbackProvider(PersonToolService personToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(personToolService)
                .build();
    }
}
