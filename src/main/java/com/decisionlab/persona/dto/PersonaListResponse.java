package com.decisionlab.persona.dto;

import com.decisionlab.persona.entity.Persona;
import com.decisionlab.persona.entity.PersonaCode;

import java.util.List;

/** 명세서 4.4 자아 목록 응답 */
public record PersonaListResponse(List<PersonaItem> personas) {

    public record PersonaItem(
            PersonaCode code,
            String name,
            String coreValue,
            String sampleQuestion,
            String themeColor
    ) {
        public static PersonaItem from(Persona persona) {
            return new PersonaItem(
                    persona.getCode(),
                    persona.getName(),
                    persona.getCoreValue(),
                    persona.getSampleQuestion(),
                    persona.getThemeColor()
            );
        }
    }
}
