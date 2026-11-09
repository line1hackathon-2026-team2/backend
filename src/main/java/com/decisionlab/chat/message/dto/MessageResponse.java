package com.decisionlab.chat.message.dto;

import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.persona.entity.PersonaCode;

import java.util.List;

/** 명세서 4.6 메시지 전송 응답 */
public record MessageResponse(
        String userMessageId,
        AiMessageItem aiMessage,
        Integer turnNo,
        boolean canProceedToDecision
) {

    public record AiMessageItem(
            String messageId,
            PersonaCode personaCode,
            String content,
            List<MessageOption> options
    ) {
        public static AiMessageItem from(AiMessage aiMessage) {
            return new AiMessageItem(
                    String.valueOf(aiMessage.messageId()),
                    aiMessage.personaCode(),
                    aiMessage.content(),
                    aiMessage.options()
            );
        }
    }
}
