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
@RequestMapping("/accounts")
@Tag(name = "Accounts", description = "LLM-orchestrated queries against account-mcp-server "
        + "(and person-mcp-server for the balance endpoint)")
public class AccountController {

    private final ChatClient chatClient;

    public AccountController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/count-by-person-id/{personId}")
    @Operation(summary = "Count accounts by person ID")
    public String countByPersonId(@PathVariable Long personId) {
        return chatClient.prompt(PromptTemplates.accountCountByPersonId(personId)).call().content();
    }

    @GetMapping("/balance-by-person-id/{personId}")
    @Operation(summary = "Report person name, nationality and total account balance by person ID",
            description = "Multi-tool orchestration: calls account-mcp-server for accounts/balances "
                    + "and person-mcp-server for name/nationality.")
    public String balanceByPersonId(@PathVariable Long personId) {
        return chatClient.prompt(PromptTemplates.accountBalanceByPersonId(personId)).call().content();
    }
}
