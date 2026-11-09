package com.decisionlab.ai.conversation;

import com.decisionlab.chat.message.dto.AiMessage;
import com.decisionlab.chat.message.entity.InputType;
import com.decisionlab.chat.message.entity.Message;
import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.chat.message.entity.MessageRole;
import com.decisionlab.chat.message.repository.MessageRepository;
import com.decisionlab.persona.entity.PersonaCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StubConversationEngine implements ConversationEngine {

    private final MessageRepository messageRepository;

    @Override
    @Transactional
    public AiMessage open(UUID sessionId, PersonaCode code) {
        Message message = messageRepository.save(Message.builder()
                .sessionId(sessionId)
                .turnNo(nextTurnNo(sessionId))
                .role(MessageRole.AI)
                .personaCode(code)
                .content("지금 고민에 대해 조금 더 말해줄래?")
                .options(List.of(
                        new MessageOption("o1", "네, 말해볼게요"),
                        new MessageOption("o2", "아직 잘 모르겠어요")
                ))
                .inputType(InputType.BUTTON)
                .build());

        return toAiMessage(message);
    }

    @Override
    @Transactional
    public AiMessage respond(UUID sessionId, PersonaCode code, String userUtterance) {
        Message message = messageRepository.save(Message.builder()
                .sessionId(sessionId)
                .turnNo(nextTurnNo(sessionId))
                .role(MessageRole.AI)
                .personaCode(code)
                .content("조금 더 구체적으로 말해줄래?")
                .options(List.of(
                        new MessageOption("o1", "네, 말해볼게요"),
                        new MessageOption("o2", "아직 잘 모르겠어요")
                ))
                .inputType(InputType.BUTTON)
                .build());

        return toAiMessage(message);
    }

    @Override
    public String createHandoffSummary(UUID sessionId) {
        return "이전 대화 내용을 바탕으로 이어서 질문합니다.";
    }

    private int nextTurnNo(UUID sessionId) {
        return messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId).size() + 1;
    }

    private AiMessage toAiMessage(Message message) {
        return new AiMessage(message.getId(), message.getPersonaCode(), message.getContent(), message.getOptions(),
                message.getTurnNo());
    }
}
