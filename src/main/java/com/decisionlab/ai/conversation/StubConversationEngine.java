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
        return saveAiMessage(sessionId, code, "지금 고민에 대해 조금 더 말해줄래?",
                List.of(
                        new MessageOption("o1", "네, 말해볼게요"),
                        new MessageOption("o2", "아직 잘 모르겠어요")
                ));
    }

    @Override
    @Transactional
    public AiMessage respond(UUID sessionId, PersonaCode code, String userUtterance) {
        return saveAiMessage(sessionId, code, "조금 더 자세히 말해줄래?",
                List.of(
                        new MessageOption("o1", "네, 더 말해볼게요"),
                        new MessageOption("o2", "여기까지면 충분해요")
                ));
    }

    @Override
    public String createHandoffSummary(UUID sessionId) {
        return "이전 대화 내용을 바탕으로 이어서 질문합니다.";
    }

    private AiMessage saveAiMessage(UUID sessionId, PersonaCode code, String content, List<MessageOption> options) {
        int nextTurnNo = messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId).size() + 1;

        Message message = messageRepository.save(Message.builder()
                .sessionId(sessionId)
                .turnNo(nextTurnNo)
                .role(MessageRole.AI)
                .personaCode(code)
                .content(content)
                .options(options)
                .inputType(InputType.BUTTON)
                .build());

        return new AiMessage(message.getId(), message.getPersonaCode(), message.getContent(), message.getOptions());
    }
}
