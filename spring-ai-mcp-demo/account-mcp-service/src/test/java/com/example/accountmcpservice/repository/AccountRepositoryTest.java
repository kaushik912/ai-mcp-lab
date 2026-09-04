package com.example.accountmcpservice.repository;

import com.example.accountmcpservice.domain.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void findByPersonIdReturnsSeededAccountsForPersonOne() {
        List<Account> accounts = accountRepository.findByPersonId(1L);

        assertThat(accounts).hasSize(2);
        assertThat(accounts.stream().mapToInt(Account::getBalance).sum()).isEqualTo(3500);
    }

    @Test
    void findByPersonIdReturnsEmptyListForUnknownPerson() {
        assertThat(accountRepository.findByPersonId(999L)).isEmpty();
    }
}
