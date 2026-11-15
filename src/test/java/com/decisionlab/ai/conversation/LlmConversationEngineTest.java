package com.decisionlab.ai.conversation;

import com.decisionlab.ai.llm.LlmClient;
import com.decisionlab.chat.message.dto.AiMessage;
import com.decisionlab.chat.message.dto.AiMessageTurn;
import com.decisionlab.chat.message.entity.InputType;
import com.decisionlab.chat.message.entity.Message;
import com.decisionlab.chat.message.entity.MessageRole;
import com.decisionlab.chat.message.repository.MessageRepository;
import com.decisionlab.concern.repository.ConcernRepository;
import com.decisionlab.persona.entity.Persona;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.persona.repository.PersonaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
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
class LlmConversationEngineTest {

    @Mock
    MessageRepository messageRepository;

    @Mock
    ConcernRepository concernRepository;

    @Mock
    PersonaRepository personaRepository;

    @Mock
    LlmClient llmClient;

    LlmConversationEngine engine;

    UUID sessionId = UUID.randomUUID();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        engine = new LlmConversationEngine(messageRepository, concernRepository, personaRepository, llmClient);
    }

    private Persona persona(PersonaCode code, String name, String coreValue) {
        return Persona.builder()
                .code(code)
                .name(name)
                .coreValue(coreValue)
                .sampleQuestion("샘플 질문")
                .themeColor("#fff")
                .build();
    }

    private Message message(int turnNo, MessageRole role, PersonaCode code, String content) {
        return Message.builder()
                .sessionId(sessionId)
                .turnNo(turnNo)
                .role(role)
                .personaCode(code)
                .content(content)
                .inputType(InputType.BUTTON)
                .build();
    }

    @SuppressWarnings("unchecked")
    @Test
    void open_firstConversation_passesFirstTimeContext() throws Exception {
        given(messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId)).willReturn(List.of());
        given(personaRepository.findByCode(PersonaCode.ACHIEVEMENT))
                .willReturn(Optional.of(persona(PersonaCode.ACHIEVEMENT, "성취의 나", "성장, 목표, 역량")));
        given(concernRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
        given(llmClient.call(eq("conversation/achievement"), any(), eq(LlmConversationEngine.ConversationTurn.class)))
                .willReturn(new LlmConversationEngine.ConversationTurn("질문", List.of("보기1", "보기2")));
        given(messageRepository.save(any())).willAnswer(invocation -> {
            Message saved = invocation.getArgument(0);
            setId(saved, 1L);
            return saved;
        });

        AiMessage result = engine.open(sessionId, PersonaCode.ACHIEVEMENT);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(llmClient).call(eq("conversation/achievement"), captor.capture(), eq(LlmConversationEngine.ConversationTurn.class));
        assertThat(captor.getValue().get("persona_name")).isEqualTo("성취의 나");
        assertThat(captor.getValue().get("core_value")).isEqualTo("성장, 목표, 역량");
        assertThat(captor.getValue().get("context").toString()).contains("첫 대화");

        assertThat(result.content()).isEqualTo("질문");
        assertThat(result.options()).hasSize(2);
        assertThat(result.options().get(0).optionId()).isEqualTo("o1");
        assertThat(result.options().get(1).optionId()).isEqualTo("o2");
    }

    @SuppressWarnings("unchecked")
    @Test
    void open_afterSwitch_passesHandoffSummaryContext() {
        Message previous = message(1, MessageRole.AI, PersonaCode.EXPLORATION, "이전 질문");
        given(messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId)).willReturn(List.of(previous));
        given(personaRepository.findByCode(PersonaCode.STABILITY))
                .willReturn(Optional.of(persona(PersonaCode.STABILITY, "안정의 나", "경제, 시간, 위험")));
        given(personaRepository.findAll())
                .willReturn(List.of(persona(PersonaCode.EXPLORATION, "탐색의 나", "가능성, 경험, 자율성")));
        given(concernRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
        given(llmClient.call(eq("conversation/stability"), any(), eq(LlmConversationEngine.ConversationTurn.class)))
                .willReturn(new LlmConversationEngine.ConversationTurn("질문2", List.of("보기1")));
        given(messageRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        engine.open(sessionId, PersonaCode.STABILITY);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(llmClient).call(eq("conversation/stability"), captor.capture(), any());
        String context = captor.getValue().get("context").toString();
        assertThat(context).contains("이전 대화 요약");
        assertThat(context).contains("탐색의 나: 이전 질문");
    }

    @SuppressWarnings("unchecked")
    @Test
    void respond_includesHistoryAndUserUtteranceInContext() {
        Message previous = message(1, MessageRole.AI, PersonaCode.ACHIEVEMENT, "첫 질문");
        given(messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId)).willReturn(List.of(previous));
        given(personaRepository.findByCode(PersonaCode.ACHIEVEMENT))
                .willReturn(Optional.of(persona(PersonaCode.ACHIEVEMENT, "성취의 나", "성장, 목표, 역량")));
        given(personaRepository.findAll())
                .willReturn(List.of(persona(PersonaCode.ACHIEVEMENT, "성취의 나", "성장, 목표, 역량")));
        given(concernRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
        given(llmClient.call(eq("conversation/achievement"), any(), eq(LlmConversationEngine.ConversationTurn.class)))
                .willReturn(new LlmConversationEngine.ConversationTurn("다음 질문", List.of("보기1", "보기2", "보기3")));
        given(messageRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        AiMessageTurn turn = engine.respond(sessionId, PersonaCode.ACHIEVEMENT, "제 답변이에요");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(llmClient).call(eq("conversation/achievement"), captor.capture(), any());
        String context = captor.getValue().get("context").toString();
        assertThat(context).contains("성취의 나: 첫 질문");
        assertThat(context).contains("제 답변이에요");

        assertThat(turn.aiMessage().content()).isEqualTo("다음 질문");
        assertThat(turn.aiMessage().options()).hasSize(3);
        assertThat(turn.turnNo()).isEqualTo(2);
    }

    @Test
    void open_truncatesOptions_whenLlmReturnsMoreThanThree() throws Exception {
        given(messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId)).willReturn(List.of());
        given(personaRepository.findByCode(PersonaCode.STABILITY))
                .willReturn(Optional.of(persona(PersonaCode.STABILITY, "안정의 나", "경제, 시간, 위험")));
        given(concernRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
        given(llmClient.call(eq("conversation/stability"), any(), eq(LlmConversationEngine.ConversationTurn.class)))
                .willReturn(new LlmConversationEngine.ConversationTurn("질문", List.of("보기1", "보기2", "보기3", "보기4")));
        given(messageRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        AiMessage result = engine.open(sessionId, PersonaCode.STABILITY);

        assertThat(result.options()).hasSize(3);
        assertThat(result.options().get(2).optionId()).isEqualTo("o3");
    }

    @Test
    void createHandoffSummary_returnsPlaceholder_whenNoHistory() {
        given(messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId)).willReturn(List.of());

        assertThat(engine.createHandoffSummary(sessionId)).isEqualTo("아직 나눈 대화가 없어.");
    }

    @Test
    void createHandoffSummary_formatsRecentHistoryWithPersonaNames() {
        Message aiMsg = message(1, MessageRole.AI, PersonaCode.EXPLORATION, "질문");
        Message userMsg = message(2, MessageRole.USER, PersonaCode.EXPLORATION, "답변");
        given(messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId)).willReturn(List.of(aiMsg, userMsg));
        given(personaRepository.findAll())
                .willReturn(List.of(persona(PersonaCode.EXPLORATION, "탐색의 나", "가능성, 경험, 자율성")));

        String summary = engine.createHandoffSummary(sessionId);

        assertThat(summary).isEqualTo("탐색의 나: 질문\n사용자: 답변");
    }

    private void setId(Object entity, Long id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
