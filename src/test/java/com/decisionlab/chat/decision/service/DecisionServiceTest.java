package com.decisionlab.chat.decision.service;

import com.decisionlab.ai.decision.DecisionExtractor;
import com.decisionlab.chat.criterion.entity.CriterionSource;
import com.decisionlab.chat.criterion.entity.JudgmentCriterion;
import com.decisionlab.chat.criterion.repository.JudgmentCriterionRepository;
import com.decisionlab.chat.decision.dto.DecisionDraftResponse;
import com.decisionlab.chat.decision.dto.DecisionDraftResult;
import com.decisionlab.chat.decision.dto.DecisionDraftResult.CriterionDraft;
import com.decisionlab.chat.decision.entity.FieldSource;
import com.decisionlab.chat.decision.entity.ProvisionalDecision;
import com.decisionlab.chat.decision.repository.ProvisionalDecisionRepository;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.service.SessionStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    @Mock
    SessionStateService sessionStateService;

    @Mock
    DecisionExtractor decisionExtractor;

    @Mock
    ProvisionalDecisionRepository provisionalDecisionRepository;

    @Mock
    JudgmentCriterionRepository judgmentCriterionRepository;

    @InjectMocks
    DecisionService decisionService;

    UUID sessionId = UUID.randomUUID();

    DecisionDraftResult stubResult() {
        return new DecisionDraftResult(
                "요약",
                "대학원 진학", FieldSource.USER_STATED,
                "연구 전문성", FieldSource.AI_INFERRED,
                "연구개발 분야 희망", FieldSource.USER_STATED,
                "취업 시점 지연", FieldSource.AI_INFERRED,
                "실제 연구 경험",
                List.of(new CriterionDraft("VALUE", "연구 전문성", CriterionSource.USER_STATED))
        );
    }

    @Test
    void draft_requiresExploringState_thenTransitionsToProvisionalDecision() throws Exception {
        given(decisionExtractor.extract(sessionId)).willReturn(stubResult());
        given(provisionalDecisionRepository.findBySessionIdOrderByRevisionNoDesc(sessionId)).willReturn(List.of());
        given(provisionalDecisionRepository.save(any())).willAnswer(invocation -> {
            ProvisionalDecision decision = invocation.getArgument(0);
            setId(decision, 10L);
            return decision;
        });
        given(judgmentCriterionRepository.save(any())).willAnswer(invocation -> {
            JudgmentCriterion criterion = invocation.getArgument(0);
            setId(criterion, 100L);
            return criterion;
        });

        DecisionDraftResponse response = decisionService.draft(sessionId);

        verify(sessionStateService).requireState(sessionId, SessionState.EXPLORING);
        verify(sessionStateService).transition(sessionId, SessionState.PROVISIONAL_DECISION);
        assertThat(response.state()).isEqualTo(SessionState.PROVISIONAL_DECISION);
        assertThat(response.decisionId()).isEqualTo("10");
        assertThat(response.revisionNo()).isEqualTo(1);
        assertThat(response.fields().decisionText()).isEqualTo("대학원 진학");
        assertThat(response.fieldSources().decisionText()).isEqualTo(FieldSource.USER_STATED);
        assertThat(response.fieldSources().coreValue()).isEqualTo(FieldSource.AI_INFERRED);
        assertThat(response.fieldSources().uncheckedCondition()).isEqualTo(FieldSource.NEEDS_CHECK);
        assertThat(response.criteria()).hasSize(1);
        assertThat(response.criteria().get(0).criterionId()).isEqualTo("100");
    }

    @Test
    void draft_incrementsRevisionNoFromPreviousDecision() {
        ProvisionalDecision previous = ProvisionalDecision.builder()
                .sessionId(sessionId)
                .revisionNo(2)
                .summaryText("이전 요약")
                .build();
        given(decisionExtractor.extract(sessionId)).willReturn(stubResult());
        given(provisionalDecisionRepository.findBySessionIdOrderByRevisionNoDesc(sessionId)).willReturn(List.of(previous));
        given(provisionalDecisionRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(judgmentCriterionRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        DecisionDraftResponse response = decisionService.draft(sessionId);

        assertThat(response.revisionNo()).isEqualTo(3);
    }

    private void setId(Object entity, Long id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
