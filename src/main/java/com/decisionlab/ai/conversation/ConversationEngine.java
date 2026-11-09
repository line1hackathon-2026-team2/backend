package com.decisionlab.ai.conversation;

import com.decisionlab.chat.message.dto.AiMessage;
import com.decisionlab.persona.entity.PersonaCode;

import java.util.UUID;

public interface ConversationEngine {

    AiMessage open(UUID sessionId, PersonaCode code);

    /** #6 메시지 전송. 사용자 발화에 대한 다음 AI 질문을 생성해 저장하고 돌려준다. */
    AiMessage respond(UUID sessionId, PersonaCode code, String userUtterance);

    String createHandoffSummary(UUID sessionId);
}
