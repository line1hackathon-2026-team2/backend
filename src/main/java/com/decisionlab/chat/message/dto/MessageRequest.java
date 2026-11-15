package com.decisionlab.chat.message.dto;

import com.decisionlab.chat.message.entity.InputType;
import jakarta.validation.constraints.NotNull;

/** 명세서 4.6 메시지 전송 요청. BUTTON/FREE_TEXT 조합 유효성은 서비스에서 검증한다. */
public record MessageRequest(
        @NotNull InputType inputType,
        String selectedOptionId,
        String content
) {
}
