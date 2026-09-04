package com.example.personmcpservice.tool;

import com.example.personmcpservice.domain.Person;
import com.example.personmcpservice.repository.PersonRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonToolService {

    private final PersonRepository personRepository;

    public PersonToolService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Tool(description = "Find person by ID")
    public Person getPersonById(@ToolParam(description = "Person ID") Long id) {
        return personRepository.findById(id).orElse(null);
    }

    @Tool(description = "Find all persons by nationality")
    public List<Person> getPersonsByNationality(@ToolParam(description = "Nationality") String nationality) {
        return personRepository.findByNationality(nationality);
    }
}
