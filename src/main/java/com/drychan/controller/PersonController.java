package com.drychan.controller;

import com.drychan.model.Person;
import com.drychan.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/persons")
@RestController
@RequiredArgsConstructor
public class PersonController {
    private final PersonService personService;

    @PostMapping("/addPerson")
    public String addPerson(@RequestParam("desc") String description) {
        Person person = new Person(description);
        personService.addPerson(person);
        return "ok";
    }
}
