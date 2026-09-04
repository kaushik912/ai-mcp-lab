package com.example.accountmcpservice.tool;

import com.example.accountmcpservice.domain.Account;
import com.example.accountmcpservice.repository.AccountRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountToolService {

    private final AccountRepository accountRepository;

    public AccountToolService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Tool(description = "Find all accounts by person ID")
    public List<Account> getAccountsByPersonId(@ToolParam(description = "Person ID") Long personId) {
        return accountRepository.findByPersonId(personId);
    }
}
