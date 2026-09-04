package com.example.personmcpservice.repository;

import com.example.personmcpservice.domain.Person;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PersonRepositoryTest {

    @Autowired
    private PersonRepository personRepository;

    @Test
    void findByNationalityReturnsSeededPersons() {
        List<Person> polishPersons = personRepository.findByNationality("Polish");

        assertThat(polishPersons)
                .extracting(Person::getFirstName)
                .containsExactlyInAnyOrder("Anna", "Piotr");
    }

    @Test
    void findByNationalityReturnsEmptyListForUnknownNationality() {
        assertThat(personRepository.findByNationality("Atlantean")).isEmpty();
    }
}
