package com.decisionlab.ai.decision;

import com.decisionlab.ai.llm.LlmClient;
import com.decisionlab.chat.decision.dto.DecisionDraftResult;
import com.decisionlab.chat.message.entity.MessageRole;
import com.decisionlab.chat.message.repository.MessageRepository;
import com.decisionlab.concern.entity.Concern;
import com.decisionlab.concern.repository.ConcernRepository;
import com.decisionlab.persona.entity.Persona;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.persona.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LlmDecisionExtractor implements DecisionExtractor {

    private final MessageRepository messageRepository;
    // A/B의 테이블은 읽기만 한다 (고민 내용, 대화 이력, 자아 이름 조회)
    private final ConcernRepository concernRepository;
    private final PersonaRepository personaRepository;
    private final LlmClient llmClient;

    @Override
    public DecisionDraftResult extract(UUID sessionId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("concern", concernText(sessionId));
        variables.put("history", historyText(sessionId));

        return llmClient.call("decision-extractor", variables, DecisionDraftResult.class);
    }

    private String concernText(UUID sessionId) {
        return concernRepository.findBySessionId(sessionId)
                .map(Concern::toPromptText)
                .orElse("아직 입력되지 않음");
    }

    private String historyText(UUID sessionId) {
        Map<PersonaCode, String> personaNames = personaRepository.findAll().stream()
                .collect(Collectors.toMap(Persona::getCode, Persona::getName));

        return messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId).stream()
                .map(m -> (m.getRole() == MessageRole.AI
                        ? personaNames.getOrDefault(m.getPersonaCode(), m.getPersonaCode().name())
                        : "사용자") + ": " + m.getContent())
                .collect(Collectors.joining("\n"));
    }
}
