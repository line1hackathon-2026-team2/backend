package com.decisionlab.ai.decision;

import com.decisionlab.ai.llm.LlmClient;
import com.decisionlab.chat.decision.dto.DecisionDraftResult;
import com.decisionlab.chat.decision.entity.FieldSource;
import com.decisionlab.chat.message.entity.InputType;
import com.decisionlab.chat.message.entity.Message;
import com.decisionlab.chat.message.entity.MessageRole;
import com.decisionlab.chat.message.repository.MessageRepository;
import com.decisionlab.concern.entity.Concern;
import com.decisionlab.concern.entity.ConcernType;
import com.decisionlab.concern.repository.ConcernRepository;
import com.decisionlab.persona.entity.Persona;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.persona.repository.PersonaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LlmDecisionExtractorTest {

    @Mock
    MessageRepository messageRepository;

    @Mock
    ConcernRepository concernRepository;

    @Mock
    PersonaRepository personaRepository;

    @Mock
    LlmClient llmClient;

    LlmDecisionExtractor extractor;

    UUID sessionId = UUID.randomUUID();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        extractor = new LlmDecisionExtractor(messageRepository, concernRepository, personaRepository, llmClient);
    }

    @SuppressWarnings("unchecked")
    @Test
    void extract_passesConcernAndHistory_thenReturnsLlmResult() {
        Message aiMsg = Message.builder()
                .sessionId(sessionId).turnNo(1).role(MessageRole.AI).personaCode(PersonaCode.ACHIEVEMENT)
                .content("질문").inputType(InputType.BUTTON).build();
        Message userMsg = Message.builder()
                .sessionId(sessionId).turnNo(2).role(MessageRole.USER).personaCode(PersonaCode.ACHIEVEMENT)
                .content("답변").inputType(InputType.FREE_TEXT).build();
        given(messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId)).willReturn(List.of(aiMsg, userMsg));
        given(personaRepository.findAll()).willReturn(List.of(Persona.builder()
                .code(PersonaCode.ACHIEVEMENT).name("성취의 나").coreValue("성장, 목표, 역량")
                .sampleQuestion("q").themeColor("#fff").build()));
        Concern concern = Concern.builder()
                .sessionId(sessionId).freeText("고민 내용").concernType(ConcernType.CAREER).build();
        given(concernRepository.findBySessionId(sessionId)).willReturn(Optional.of(concern));

        DecisionDraftResult expected = new DecisionDraftResult(
                "요약", "결정", FieldSource.USER_STATED, "가치", FieldSource.AI_INFERRED,
                "이유", FieldSource.USER_STATED, "unknown", null, "조건", List.of());
        given(llmClient.call(eq("decision-extractor"), any(), eq(DecisionDraftResult.class))).willReturn(expected);

        DecisionDraftResult result = extractor.extract(sessionId);

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(llmClient).call(eq("decision-extractor"), captor.capture(), eq(DecisionDraftResult.class));
        assertThat(captor.getValue().get("concern")).isEqualTo(concern.toPromptText());
        String history = captor.getValue().get("history").toString();
        assertThat(history).isEqualTo("성취의 나: 질문\n사용자: 답변");
    }

    @Test
    void extract_whenConcernMissing_usesPlaceholderText() {
        given(messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId)).willReturn(List.of());
        given(concernRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
        given(llmClient.call(eq("decision-extractor"), any(), eq(DecisionDraftResult.class)))
                .willReturn(new DecisionDraftResult("s", null, null, null, null, null, null, null, null, "c", List.of()));

        extractor.extract(sessionId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(llmClient).call(eq("decision-extractor"), captor.capture(), eq(DecisionDraftResult.class));
        assertThat(captor.getValue().get("concern")).isEqualTo("아직 입력되지 않음");
        assertThat(captor.getValue().get("history")).isEqualTo("");
    }
}
