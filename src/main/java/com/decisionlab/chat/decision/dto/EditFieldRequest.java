package com.decisionlab.chat.decision.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 명세서 4.9 잠정 결정 필드 수정 요청 (P1) */
public record EditFieldRequest(
        @NotBlank String field,
        @NotBlank @Size(min = 1, max = 200) String value
) {
}
