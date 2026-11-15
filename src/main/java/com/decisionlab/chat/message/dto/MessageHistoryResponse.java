package com.decisionlab.chat.message.dto;

import com.decisionlab.chat.message.entity.InputType;
import com.decisionlab.chat.message.entity.Message;
import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.chat.message.entity.MessageRole;
import com.decisionlab.global.time.ApiTime;
import com.decisionlab.persona.entity.PersonaCode;

import java.time.OffsetDateTime;
import java.util.List;

/** 명세서 4.7 대화 이력 조회 응답 */
public record MessageHistoryResponse(List<MessageItem> messages) {

    public record MessageItem(
            String messageId,
            MessageRole role,
            PersonaCode personaCode,
            String content,
            List<MessageOption> options,
            InputType inputType,
            Integer turnNo,
            OffsetDateTime createdAt
    ) {
        public static MessageItem from(Message message) {
            return new MessageItem(
                    String.valueOf(message.getId()),
                    message.getRole(),
                    message.getPersonaCode(),
                    message.getContent(),
                    message.getOptions(),
                    message.getInputType(),
                    message.getTurnNo(),
                    ApiTime.toKst(message.getCreatedAt())
            );
        }
    }
}
