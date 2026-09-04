package com.example.accountmcpservice.config;

import com.example.accountmcpservice.tool.AccountToolService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolCallbackProviderConfig {

    @Bean
    public ToolCallbackProvider accountToolCallbackProvider(AccountToolService accountToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(accountToolService)
                .build();
    }
}
