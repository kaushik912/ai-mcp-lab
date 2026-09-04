package com.example.sampleclient.web;

import com.example.sampleclient.prompt.PromptTemplates;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/persons")
@Tag(name = "Persons", description = "LLM-orchestrated queries against person-mcp-server")
public class PersonController {

    private final ChatClient chatClient;

    public PersonController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/nationality/{nationality}")
    @Operation(summary = "Find persons by nationality")
    public String findByNationality(@PathVariable String nationality) {
        return chatClient.prompt(PromptTemplates.personsByNationality(nationality)).call().content();
    }

    @GetMapping("/count-by-nationality/{nationality}")
    @Operation(summary = "Count persons by nationality")
    public String countByNationality(@PathVariable String nationality) {
        return chatClient.prompt(PromptTemplates.personCountByNationality(nationality)).call().content();
    }
}
