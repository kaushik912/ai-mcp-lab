package com.example.personmcpservice.repository;

import com.example.personmcpservice.domain.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonRepository extends JpaRepository<Person, Long> {

    List<Person> findByNationality(String nationality);
}
