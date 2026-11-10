package com.decisionlab.persona.dto;

import com.decisionlab.chat.message.dto.AiMessage;
import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.session.entity.SessionState;

import java.util.List;

/** 명세서 4.5 자아 선택/전환 응답. messageId는 숫자 문자열  */
public record PersonaSelectResponse(
        SessionState state,
        PersonaCode personaCode,
        OpeningMessage openingMessage
) {

    public record OpeningMessage(String messageId, String content, List<MessageOption> options) {

        public static OpeningMessage from(AiMessage message) {
            return new OpeningMessage(
                    String.valueOf(message.messageId()),
                    message.content(),
                    message.options() == null ? List.of() : message.options()
            );
        }
    }
}
