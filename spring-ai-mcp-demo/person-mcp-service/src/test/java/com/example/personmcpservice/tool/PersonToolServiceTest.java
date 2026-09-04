package com.example.personmcpservice.tool;

import com.example.personmcpservice.domain.Gender;
import com.example.personmcpservice.domain.Person;
import com.example.personmcpservice.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonToolServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Test
    void getPersonByIdReturnsPersonWhenFound() {
        Person person = new Person("John", "Smith", 40, "American", Gender.MALE);
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));

        PersonToolService toolService = new PersonToolService(personRepository);

        assertThat(toolService.getPersonById(1L)).isSameAs(person);
    }

    @Test
    void getPersonByIdReturnsNullWhenNotFound() {
        when(personRepository.findById(99L)).thenReturn(Optional.empty());

        PersonToolService toolService = new PersonToolService(personRepository);

        assertThat(toolService.getPersonById(99L)).isNull();
    }

    @Test
    void getPersonsByNationalityDelegatesToRepository() {
        Person person = new Person("Anna", "Kowalska", 35, "Polish", Gender.FEMALE);
        when(personRepository.findByNationality("Polish")).thenReturn(List.of(person));

        PersonToolService toolService = new PersonToolService(personRepository);

        assertThat(toolService.getPersonsByNationality("Polish")).containsExactly(person);
        verify(personRepository).findByNationality("Polish");
    }
}
