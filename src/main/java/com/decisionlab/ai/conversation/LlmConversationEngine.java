package com.decisionlab.ai.conversation;

import com.decisionlab.ai.llm.LlmClient;
import com.decisionlab.chat.message.dto.AiMessage;
import com.decisionlab.chat.message.dto.AiMessageTurn;
import com.decisionlab.chat.message.entity.InputType;
import com.decisionlab.chat.message.entity.Message;
import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.chat.message.entity.MessageRole;
import com.decisionlab.chat.message.repository.MessageRepository;
import com.decisionlab.concern.entity.Concern;
import com.decisionlab.concern.repository.ConcernRepository;
import com.decisionlab.persona.entity.Persona;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.persona.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LlmConversationEngine implements ConversationEngine {

    // 핸드오프 요약에 포함할 최근 대화 턴 수
    private static final int HANDOFF_HISTORY_TURNS = 4;
    // 명세서 4.6: 선택지는 2~3개. 프롬프트로만 강제하지 않고 코드에서도 상한을 둔다.
    private static final int MAX_OPTIONS = 3;

    private final MessageRepository messageRepository;
    // A의 테이블은 읽기만 한다 (고민 내용, 자아 이름/핵심 가치 조회)
    private final ConcernRepository concernRepository;
    private final PersonaRepository personaRepository;
    private final LlmClient llmClient;

    @Override
    @Transactional
    public AiMessage open(UUID sessionId, PersonaCode code) {
        List<Message> history = messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId);
        String context = history.isEmpty()
                ? "이번이 이 자아와의 첫 대화야. 가볍게 고민에 대해 더 물어봐."
                : "사용자가 다른 자아와 대화하다가 방금 너에게로 넘어왔어. 아래는 이전 대화 요약이야.\n이전 대화 요약:\n" + createHandoffSummary(sessionId)
                        + "\n\ncontent는 두 문장으로 구성해: "
                        + "① 이전 대화 요약에 나온 자아 이름과 핵심 내용을 한 문장으로 짚어줘(예: \"OO의 나와 이야기하며 ~라고 했지.\"). "
                        + "② 그 내용이 지금 네 관점에서 왜 신경 쓰이는지 짧게 연결하면서, 맞는지 확인하는 질문으로 끝내(예: \"...하는 거야?\"). "
                        + "options는 그 확인 질문에 대한 대답(맞아/아니야 계열)으로 2~3개 제시해.";

        return toAiMessage(generateAndSave(sessionId, code, context));
    }

    @Override
    @Transactional
    public AiMessageTurn respond(UUID sessionId, PersonaCode code, String userUtterance) {
        List<Message> history = messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId);
        String context = "지금까지 나눈 대화:\n" + historyText(history)
                + "\n\n사용자의 방금 답변: " + userUtterance
                + "\n이 답변을 참고해서 다음 질문을 해.";

        Message message = generateAndSave(sessionId, code, context);
        return new AiMessageTurn(toAiMessage(message), message.getTurnNo());
    }

    @Override
    public String createHandoffSummary(UUID sessionId) {
        List<Message> history = messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId);
        if (history.isEmpty()) {
            return "아직 나눈 대화가 없어.";
        }
        List<Message> recent = history.subList(Math.max(0, history.size() - HANDOFF_HISTORY_TURNS), history.size());
        return historyText(recent);
    }

    private Message generateAndSave(UUID sessionId, PersonaCode code, String context) {
        Persona persona = personaRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("persona 시드가 없습니다: " + code));

        Map<String, Object> variables = new HashMap<>();
        variables.put("persona_name", persona.getName());
        variables.put("core_value", persona.getCoreValue());
        variables.put("concern", concernText(sessionId));
        variables.put("context", context);

        ConversationTurn turn = llmClient.call(
                "conversation/" + code.name().toLowerCase(), variables, ConversationTurn.class);

        return messageRepository.save(Message.builder()
                .sessionId(sessionId)
                .turnNo(nextTurnNo(sessionId))
                .role(MessageRole.AI)
                .personaCode(code)
                .content(turn.content())
                .options(toOptions(turn.options()))
                .inputType(InputType.BUTTON)
                .build());
    }

    private List<MessageOption> toOptions(List<String> labels) {
        List<MessageOption> options = new ArrayList<>();
        int count = Math.min(labels.size(), MAX_OPTIONS);
        for (int i = 0; i < count; i++) {
            options.add(new MessageOption("o" + (i + 1), labels.get(i)));
        }
        return options;
    }

    private String concernText(UUID sessionId) {
        return concernRepository.findBySessionId(sessionId)
                .map(Concern::toPromptText)
                .orElse("아직 입력되지 않음");
    }

    private String historyText(List<Message> messages) {
        Map<PersonaCode, String> personaNames = personaRepository.findAll().stream()
                .collect(Collectors.toMap(Persona::getCode, Persona::getName));

        return messages.stream()
                .map(m -> (m.getRole() == MessageRole.AI
                        ? personaNames.getOrDefault(m.getPersonaCode(), m.getPersonaCode().name())
                        : "사용자") + ": " + m.getContent())
                .collect(Collectors.joining("\n"));
    }

    private int nextTurnNo(UUID sessionId) {
        return messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId).size() + 1;
    }

    private AiMessage toAiMessage(Message message) {
        return new AiMessage(message.getId(), message.getPersonaCode(), message.getContent(), message.getOptions());
    }

    /** LLM 구조화 출력 전용 DTO. 질문 1개 + 보기 2~3개. */
    public record ConversationTurn(String content, List<String> options) {
    }
}
