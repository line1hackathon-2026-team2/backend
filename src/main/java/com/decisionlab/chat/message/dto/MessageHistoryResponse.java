package com.decisionlab.chat.message.dto;

import com.decisionlab.chat.message.entity.InputType;
import com.decisionlab.chat.message.entity.Message;
import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.chat.message.entity.MessageRole;
import com.decisionlab.persona.entity.PersonaCode;

import java.time.LocalDateTime;
import java.util.List;

public record MessageHistoryResponse(List<MessageItem> messages) {

    public record MessageItem(
            Long messageId,
            MessageRole role,
            PersonaCode personaCode,
            String content,
            List<MessageOption> options,
            InputType inputType,
            Integer turnNo,
            LocalDateTime createdAt
    ) {
        public static MessageItem from(Message message) {
            return new MessageItem(
                    message.getId(),
                    message.getRole(),
                    message.getPersonaCode(),
                    message.getContent(),
                    message.getOptions() == null ? List.of() : message.getOptions(),
                    message.getInputType(),
                    message.getTurnNo(),
                    message.getCreatedAt()
            );
        }
    }
}
