package com.example.personmcpservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ToolCallbackProviderConfigTest {

    @Autowired
    private ToolCallbackProvider toolCallbackProvider;

    @Test
    void exposesPersonTools() {
        String[] toolNames = Stream.of(toolCallbackProvider.getToolCallbacks())
                .map(ToolCallback::getToolDefinition)
                .map(definition -> definition.name())
                .toArray(String[]::new);

        assertThat(toolNames).containsExactlyInAnyOrder("getPersonById", "getPersonsByNationality");
    }
}
