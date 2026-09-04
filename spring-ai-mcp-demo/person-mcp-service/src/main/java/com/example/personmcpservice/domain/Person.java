package com.example.personmcpservice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private int age;
    private String nationality;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    protected Person() {
    }

    public Person(String firstName, String lastName, int age, String nationality, Gender gender) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.nationality = nationality;
        this.gender = gender;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public int getAge() {
        return age;
    }

    public String getNationality() {
        return nationality;
    }

    public Gender getGender() {
        return gender;
    }
}
