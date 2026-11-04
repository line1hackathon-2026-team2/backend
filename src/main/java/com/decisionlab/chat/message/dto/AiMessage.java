package com.decisionlab.chat.message.dto;

import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.persona.entity.PersonaCode;

import java.util.List;

public record AiMessage(
        Long messageId,
        PersonaCode personaCode,
        String content,
        List<MessageOption> options
) {
}
