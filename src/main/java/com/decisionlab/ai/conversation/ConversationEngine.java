package com.decisionlab.ai.conversation;

import com.decisionlab.chat.message.dto.AiMessage;
import com.decisionlab.persona.entity.PersonaCode;

import java.util.UUID;

public interface ConversationEngine {

    AiMessage open(UUID sessionId, PersonaCode code);

    String createHandoffSummary(UUID sessionId);
}
