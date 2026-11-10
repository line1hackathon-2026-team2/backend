package com.decisionlab.persona.controller;

import com.decisionlab.persona.dto.PersonaSelectRequest;
import com.decisionlab.persona.dto.PersonaSelectResponse;
import com.decisionlab.persona.service.PersonaSelectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}")
public class PersonaSelectController {

    private final PersonaSelectService personaSelectService;

    public PersonaSelectController(PersonaSelectService personaSelectService) {
        this.personaSelectService = personaSelectService;
    }

    /** #5 자아 선택/전환. 경로의 sessionId와 토큰의 세션이 같은지는 SessionTokenFilter가 확인한다. */
    @PostMapping("/persona")
    public PersonaSelectResponse selectPersona(@PathVariable UUID sessionId,
                                               @Valid @RequestBody PersonaSelectRequest request) {
        return personaSelectService.selectPersona(sessionId, request);
    }
}
