package com.example.accountmcpservice.tool;

import com.example.accountmcpservice.domain.Account;
import com.example.accountmcpservice.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountToolServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Test
    void getAccountsByPersonIdDelegatesToRepository() {
        Account account = new Account("PL01", 1000, 1L);
        when(accountRepository.findByPersonId(1L)).thenReturn(List.of(account));

        AccountToolService toolService = new AccountToolService(accountRepository);

        assertThat(toolService.getAccountsByPersonId(1L)).containsExactly(account);
        verify(accountRepository).findByPersonId(1L);
    }
}
