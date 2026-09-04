package com.example.accountmcpservice.repository;

import com.example.accountmcpservice.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByPersonId(Long personId);
}
