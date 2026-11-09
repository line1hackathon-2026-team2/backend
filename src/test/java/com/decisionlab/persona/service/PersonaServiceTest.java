package com.decisionlab.persona.service;

import com.decisionlab.persona.dto.PersonaListResponse;
import com.decisionlab.persona.dto.PersonaListResponse.PersonaItem;
import com.decisionlab.persona.entity.Persona;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.persona.repository.PersonaRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersonaServiceTest {

    private final PersonaRepository personaRepository = mock(PersonaRepository.class);
    private final PersonaService personaService = new PersonaService(personaRepository);

    private Persona persona(PersonaCode code) {
        return Persona.builder()
                .code(code)
                .name(code.name() + "-name")
                .coreValue("value")
                .sampleQuestion("question")
                .themeColor("#FFFFFF")
                .build();
    }

    @Test
    void 딜레마_관리자를_빼고_성취_안정_탐색_순서로_돌려준다() {
        when(personaRepository.findAll()).thenReturn(List.of(
                persona(PersonaCode.DILEMMA_MANAGER),
                persona(PersonaCode.EXPLORATION),
                persona(PersonaCode.ACHIEVEMENT),
                persona(PersonaCode.STABILITY)
        ));

        PersonaListResponse response = personaService.getSelectablePersonas();

        assertThat(response.personas()).extracting(PersonaItem::code)
                .containsExactly(PersonaCode.ACHIEVEMENT, PersonaCode.STABILITY, PersonaCode.EXPLORATION);
    }

    @Test
    void 시드가_없으면_빈_목록() {
        when(personaRepository.findAll()).thenReturn(List.of());

        assertThat(personaService.getSelectablePersonas().personas()).isEmpty();
    }

    @Test
    void DILEMMA_MANAGER만_선택_불가() {
        assertThat(PersonaCode.ACHIEVEMENT.isSelectable()).isTrue();
        assertThat(PersonaCode.STABILITY.isSelectable()).isTrue();
        assertThat(PersonaCode.EXPLORATION.isSelectable()).isTrue();
        assertThat(PersonaCode.DILEMMA_MANAGER.isSelectable()).isFalse();
    }
}
