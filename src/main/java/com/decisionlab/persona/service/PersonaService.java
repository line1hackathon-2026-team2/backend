package com.decisionlab.persona.service;

import com.decisionlab.persona.dto.PersonaListResponse;
import com.decisionlab.persona.dto.PersonaListResponse.PersonaItem;
import com.decisionlab.persona.entity.Persona;
import com.decisionlab.persona.repository.PersonaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class PersonaService {

    private final PersonaRepository personaRepository;

    public PersonaService(PersonaRepository personaRepository) {
        this.personaRepository = personaRepository;
    }

    /** 선택 가능한 자아만 enum 선언 순서(성취 → 안정 → 탐색)로 돌려준다. */
    @Transactional(readOnly = true)
    public PersonaListResponse getSelectablePersonas() {
        List<PersonaItem> personas = personaRepository.findAll().stream()
                .filter(persona -> persona.getCode().isSelectable())
                .sorted(Comparator.comparing(Persona::getCode))
                .map(PersonaItem::from)
                .toList();
        return new PersonaListResponse(personas);
    }
}
