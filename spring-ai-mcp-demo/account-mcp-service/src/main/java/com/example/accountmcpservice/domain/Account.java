package com.example.accountmcpservice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String number;
    private int balance;
    private Long personId;

    protected Account() {
    }

    public Account(String number, int balance, Long personId) {
        this.number = number;
        this.balance = balance;
        this.personId = personId;
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public int getBalance() {
        return balance;
    }

    public Long getPersonId() {
        return personId;
    }
}
