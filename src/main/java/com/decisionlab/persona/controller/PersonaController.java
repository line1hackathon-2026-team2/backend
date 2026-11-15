package com.decisionlab.persona.controller;

import com.decisionlab.persona.dto.PersonaListResponse;
import com.decisionlab.persona.service.PersonaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/personas")
public class PersonaController {

    private final PersonaService personaService;

    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }

    /** #4 자아 카드 목록. 토큰 없이 호출 가능. */
    @GetMapping
    public PersonaListResponse getPersonas() {
        return personaService.getSelectablePersonas();
    }
}
