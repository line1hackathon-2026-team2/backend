package com.decisionlab.persona.dto;

import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.persona.entity.SwitchReason;
import jakarta.validation.constraints.NotNull;

/** 명세서 4.5 자아 선택/전환 요청 */
public record PersonaSelectRequest(
        @NotNull(message = "자아는 필수입니다.")
        PersonaCode personaCode,

        @NotNull(message = "선택 이유는 필수입니다.")
        SwitchReason reason
) {
}
