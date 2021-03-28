package com.drychan.service;

import com.drychan.model.Person;
import com.drychan.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersonRepository personRepository;

    @Transactional
    public void addPerson(Person person) {
        personRepository.save(person);
    }
}
