package com.decisionlab.ai.dilemma;

import com.decisionlab.ai.dilemma.LlmDilemmaGenerator.GeneratedDilemma;
import com.decisionlab.ai.llm.LlmClient;
import com.decisionlab.chat.criterion.repository.JudgmentCriterionRepository;
import com.decisionlab.chat.decision.entity.ProvisionalDecision;
import com.decisionlab.chat.decision.repository.ProvisionalDecisionRepository;
import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.concern.repository.ConcernRepository;
import com.decisionlab.dilemma.dto.DilemmaResult;
import com.decisionlab.dilemma.entity.Dilemma;
import com.decisionlab.dilemma.entity.DilemmaTemplate;
import com.decisionlab.dilemma.entity.DilemmaType;
import com.decisionlab.dilemma.repository.DilemmaRepository;
import com.decisionlab.dilemma.repository.DilemmaTemplateRepository;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LlmDilemmaGeneratorTest {

    @Mock
    DilemmaRepository dilemmaRepository;

    @Mock
    DilemmaTemplateRepository dilemmaTemplateRepository;

    @Mock
    ConcernRepository concernRepository;

    @Mock
    ProvisionalDecisionRepository provisionalDecisionRepository;

    @Mock
    JudgmentCriterionRepository judgmentCriterionRepository;

    @Mock
    LlmClient llmClient;

    @InjectMocks
    LlmDilemmaGenerator llmDilemmaGenerator;

    private final UUID sessionId = UUID.randomUUID();
    private final Long decisionId = 10L;

    @Test
    void LLM_호출_성공_시_생성된_딜레마와_흔들린_가치_선택지를_저장하고_반환한다() {
        given(dilemmaRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
        given(concernRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
        given(provisionalDecisionRepository.findById(decisionId)).willReturn(Optional.of(
                ProvisionalDecision.builder()
                        .sessionId(sessionId)
                        .revisionNo(1)
                        .summaryText("대학원 진학 요약")
                        .decisionText("대학원 진학")
                        .coreValue("연구 전문성")
                        .build()
        ));
        given(judgmentCriterionRepository.findByDecisionId(decisionId)).willReturn(List.of());

        GeneratedDilemma generated = new GeneratedDilemma(
                DilemmaType.ALT_PATH,
                "학위 없이도 전문성을 쌓을 수 있다면?",
                "[가상 상황] 희망하던 기업에서 연구개발 직무를 제안했습니다. 지금의 판단은 달라질까요?",
                "전문성을 얻는 방법이 반드시 대학원이어야 하는가",
                List.of("좋은 기업에서 일할 기회를 포기하는 게 아까워졌어요", "대학원이 아니어도 연구 역량을 쌓을 수 있을 것 같아요")
        );
        given(llmClient.call(eq("dilemma-generator"), any(), eq(GeneratedDilemma.class))).willReturn(generated);
        given(dilemmaRepository.save(any(Dilemma.class))).willAnswer(invocation -> {
            Dilemma d = invocation.getArgument(0);
            setField(Dilemma.class, d, "id", 1L);
            return d;
        });

        DilemmaResult result = llmDilemmaGenerator.generate(sessionId, decisionId);

        ArgumentCaptor<Dilemma> captor = ArgumentCaptor.forClass(Dilemma.class);
        verify(dilemmaRepository).save(captor.capture());
        Dilemma saved = captor.getValue();

        assertThat(saved.isFallback()).isFalse();
        assertThat(saved.getTitle()).isEqualTo("학위 없이도 전문성을 쌓을 수 있다면?");
        assertThat(saved.getShakenValueOptions()).hasSize(2);
        assertThat(saved.getShakenValueOptions().get(0).optionId()).isEqualTo("o1");
        assertThat(saved.getShakenValueOptions().get(0).label()).isEqualTo("좋은 기업에서 일할 기회를 포기하는 게 아까워졌어요");

        assertThat(result.dilemmaId()).isEqualTo(1L);
        assertThat(result.dilemmaType()).isEqualTo(DilemmaType.ALT_PATH);
        assertThat(result.title()).isEqualTo("학위 없이도 전문성을 쌓을 수 있다면?");
        assertThat(result.isFallback()).isFalse();
    }

    @Test
    void LLM_호출_실패_시_예외를_던지지_않고_템플릿으로_폴백한다() {
        given(dilemmaRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
        given(concernRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
        given(provisionalDecisionRepository.findById(decisionId)).willReturn(Optional.empty());
        given(judgmentCriterionRepository.findByDecisionId(decisionId)).willReturn(List.of());
        given(llmClient.call(eq("dilemma-generator"), any(), eq(GeneratedDilemma.class)))
                .willThrow(new ApiException(ErrorCode.LLM_FAILED, "LLM 호출 실패"));

        DilemmaTemplate template = DilemmaTemplate.builder()
                .dilemmaType(DilemmaType.ALT_PATH)
                .title("템플릿 딜레마 제목")
                .scenarioText("[가상 상황] 템플릿 시나리오")
                .verificationGoal("템플릿 검증 목표")
                .shakenValueOptions(List.of(
                        new MessageOption("o1", "템플릿 보기 1"),
                        new MessageOption("o2", "템플릿 보기 2")
                ))
                .build();
        given(dilemmaTemplateRepository.findByDilemmaType(DilemmaType.ALT_PATH)).willReturn(Optional.of(template));
        given(dilemmaRepository.save(any(Dilemma.class))).willAnswer(invocation -> {
            Dilemma d = invocation.getArgument(0);
            setField(Dilemma.class, d, "id", 2L);
            return d;
        });

        DilemmaResult result = llmDilemmaGenerator.generate(sessionId, decisionId);

        assertThat(result.dilemmaId()).isEqualTo(2L);
        assertThat(result.title()).isEqualTo("템플릿 딜레마 제목");
        assertThat(result.isFallback()).isTrue();
    }

    @Test
    void LLM_응답의_shakenValueOptions가_부족하면_해당_유형의_템플릿_보기로_폴백한다() {
        given(dilemmaRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
        given(concernRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
        given(provisionalDecisionRepository.findById(decisionId)).willReturn(Optional.empty());
        given(judgmentCriterionRepository.findByDecisionId(decisionId)).willReturn(List.of());

        GeneratedDilemma generated = new GeneratedDilemma(
                DilemmaType.VALUE_CONFLICT,
                "딜레마 제목",
                "[가상 상황] 시나리오",
                "검증 목표",
                List.of()
        );
        given(llmClient.call(eq("dilemma-generator"), any(), eq(GeneratedDilemma.class))).willReturn(generated);

        DilemmaTemplate template = DilemmaTemplate.builder()
                .dilemmaType(DilemmaType.VALUE_CONFLICT)
                .title("템플릿 제목")
                .scenarioText("템플릿 시나리오")
                .verificationGoal("템플릿 목표")
                .shakenValueOptions(List.of(
                        new MessageOption("o1", "템플릿 폴백 보기 1"),
                        new MessageOption("o2", "템플릿 폴백 보기 2")
                ))
                .build();
        given(dilemmaTemplateRepository.findByDilemmaType(DilemmaType.VALUE_CONFLICT))
                .willReturn(Optional.of(template));
        given(dilemmaRepository.save(any(Dilemma.class))).willAnswer(invocation -> {
            Dilemma d = invocation.getArgument(0);
            setField(Dilemma.class, d, "id", 3L);
            return d;
        });

        llmDilemmaGenerator.generate(sessionId, decisionId);

        ArgumentCaptor<Dilemma> captor = ArgumentCaptor.forClass(Dilemma.class);
        verify(dilemmaRepository).save(captor.capture());
        assertThat(captor.getValue().getShakenValueOptions()).hasSize(2);
        assertThat(captor.getValue().getShakenValueOptions().get(0).label()).isEqualTo("템플릿 폴백 보기 1");
    }

    @Test
    void 이미_세션에_딜레마가_존재하면_LLM을_호출하지_않고_기존_딜레마를_반환한다() {
        Dilemma existing = Dilemma.builder()
                .sessionId(sessionId)
                .decisionId(decisionId)
                .dilemmaType(DilemmaType.ALT_PATH)
                .title("기존 딜레마 제목")
                .scenarioText("기존 시나리오")
                .verificationGoal("기존 목표")
                .fallback(false)
                .build();
        setField(Dilemma.class, existing, "id", 5L);
        given(dilemmaRepository.findBySessionId(sessionId)).willReturn(Optional.of(existing));

        DilemmaResult result = llmDilemmaGenerator.generate(sessionId, decisionId);

        verify(llmClient, never()).call(any(), any(), any());
        assertThat(result.dilemmaId()).isEqualTo(5L);
        assertThat(result.title()).isEqualTo("기존 딜레마 제목");
    }

    private static void setField(Class<?> clazz, Object target, String fieldName, Object value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
