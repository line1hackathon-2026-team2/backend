package com.decisionlab.chat.decision.service;

import com.decisionlab.ai.decision.DecisionExtractor;
import com.decisionlab.ai.dilemma.DilemmaGenerator;
import com.decisionlab.ai.report.ReportGenerator;
import com.decisionlab.chat.criterion.entity.CriterionSource;
import com.decisionlab.chat.criterion.entity.JudgmentCriterion;
import com.decisionlab.chat.criterion.repository.JudgmentCriterionRepository;
import com.decisionlab.chat.decision.dto.DecisionDraftResponse;
import com.decisionlab.chat.decision.dto.DecisionDraftResult;
import com.decisionlab.chat.decision.dto.DecisionDraftResult.CriterionDraft;
import com.decisionlab.chat.decision.dto.EditFieldRequest;
import com.decisionlab.chat.decision.dto.EditFieldResponse;
import com.decisionlab.chat.decision.dto.ResolveRequest;
import com.decisionlab.chat.decision.dto.ResolveResponse;
import com.decisionlab.chat.decision.entity.DecisionAction;
import com.decisionlab.chat.decision.entity.FieldSource;
import com.decisionlab.chat.decision.entity.ProvisionalDecision;
import com.decisionlab.chat.decision.repository.ProvisionalDecisionRepository;
import com.decisionlab.dilemma.dto.DilemmaResult;
import com.decisionlab.dilemma.entity.Dilemma;
import com.decisionlab.dilemma.entity.DilemmaType;
import com.decisionlab.dilemma.repository.DilemmaRepository;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.report.dto.ReportResult;
import com.decisionlab.report.entity.EntryPoint;
import com.decisionlab.report.entity.ReportType;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.service.SessionStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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

    @Mock
    DilemmaGenerator dilemmaGenerator;

    @Mock
    ReportGenerator reportGenerator;

    @Mock
    DilemmaRepository dilemmaRepository;

    @InjectMocks
    DecisionService decisionService;

    UUID sessionId = UUID.randomUUID();
    Long decisionId = 10L;

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

    private ProvisionalDecision decision() throws Exception {
        ProvisionalDecision decision = ProvisionalDecision.builder()
                .sessionId(sessionId)
                .revisionNo(1)
                .summaryText("요약")
                .build();
        setId(decision, decisionId);
        return decision;
    }

    private ProvisionalDecision decision(UUID ownerSessionId) throws Exception {
        ProvisionalDecision decision = ProvisionalDecision.builder()
                .sessionId(ownerSessionId)
                .revisionNo(1)
                .summaryText("요약")
                .decisionText("대학원 진학")
                .coreValue("연구 전문성")
                .reason("연구개발 분야 희망")
                .acceptableCost("취업 시점 지연")
                .uncheckedCondition("실제 연구 경험")
                .build();
        setId(decision, decisionId);
        return decision;
    }

    @Test
    void editField_updatesOnlyRequestedField_keepsRevisionNo() throws Exception {
        given(provisionalDecisionRepository.findById(decisionId)).willReturn(Optional.of(decision(sessionId)));

        EditFieldResponse response = decisionService.editField(sessionId, decisionId,
                new EditFieldRequest("coreValue", "경제적 안정"));

        verify(sessionStateService).requireState(sessionId, SessionState.PROVISIONAL_DECISION);
        assertThat(response.decisionId()).isEqualTo("10");
        assertThat(response.revisionNo()).isEqualTo(1);
        assertThat(response.fields().coreValue()).isEqualTo("경제적 안정");
        assertThat(response.fields().decisionText()).isEqualTo("대학원 진학");
    }

    @Test
    void editField_withUnknownField_throwsValidationError() {
        assertThatThrownBy(() -> decisionService.editField(sessionId, decisionId,
                new EditFieldRequest("unknownField", "값")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void editField_withDecisionFromAnotherSession_throwsNotFound() throws Exception {
        given(provisionalDecisionRepository.findById(decisionId)).willReturn(Optional.of(decision(UUID.randomUUID())));

        assertThatThrownBy(() -> decisionService.editField(sessionId, decisionId,
                new EditFieldRequest("coreValue", "경제적 안정")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void resolve_approve_firstTime_generatesDilemmaAndTransitionsToDilemma() throws Exception {
        given(provisionalDecisionRepository.findById(decisionId)).willReturn(Optional.of(decision()));
        given(dilemmaRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
        DilemmaResult dilemmaResult = new DilemmaResult(1L, DilemmaType.ALT_PATH, "제목", "시나리오", "검증 목표", false);
        given(dilemmaGenerator.generate(sessionId, decisionId)).willReturn(dilemmaResult);

        ResolveResponse response = decisionService.resolve(sessionId, decisionId, new ResolveRequest(DecisionAction.APPROVE));

        verify(sessionStateService).requireState(sessionId, SessionState.PROVISIONAL_DECISION);
        verify(sessionStateService).transition(sessionId, SessionState.DILEMMA);
        verify(reportGenerator, never()).generate(any(), any(), any());
        assertThat(response.state()).isEqualTo(SessionState.DILEMMA);
        assertThat(response.dilemma().dilemmaId()).isEqualTo("1");
        assertThat(response.dilemma().title()).isEqualTo("제목");
        assertThat(response.dilemma().scenarioText()).isEqualTo("시나리오");
        assertThat(response.reportId()).isNull();
    }

    @Test
    void resolve_approve_withExistingDilemma_skipsDilemmaAndGoesToReport() throws Exception {
        given(provisionalDecisionRepository.findById(decisionId)).willReturn(Optional.of(decision()));
        Dilemma dilemma = Dilemma.builder()
                .sessionId(sessionId)
                .decisionId(1L)
                .dilemmaType(DilemmaType.ALT_PATH)
                .scenarioText("시나리오")
                .verificationGoal("목표")
                .fallback(false)
                .build();
        setId(dilemma, 5L);
        given(dilemmaRepository.findBySessionId(sessionId)).willReturn(Optional.of(dilemma));
        given(reportGenerator.generate(sessionId, ReportType.REPORT, EntryPoint.S05))
                .willReturn(reportResult(99L));

        ResolveResponse response = decisionService.resolve(sessionId, decisionId, new ResolveRequest(DecisionAction.APPROVE));

        verify(sessionStateService).transition(sessionId, SessionState.REPORT);
        verify(dilemmaGenerator, never()).generate(any(), any());
        assertThat(response.state()).isEqualTo(SessionState.REPORT);
        assertThat(response.reportId()).isEqualTo("99");
        assertThat(response.dilemma()).isNull();
    }

    /**
     * PR #30 리뷰 코멘트: KEEP으로 REPORT까지 간 세션(RECONSIDER 없음)을 reopen 후
     * 다시 APPROVE하면, reconsiderCount 기준으로는 "최초 승인"으로 오판해 딜레마를
     * 다시 만들려다 dilemmas.session_id UNIQUE 제약에 걸릴 수 있었던 케이스.
     */
    @Test
    void resolve_approve_afterReopenWithoutReconsider_stillSkipsDilemmaRegeneration() throws Exception {
        given(provisionalDecisionRepository.findById(decisionId)).willReturn(Optional.of(decision()));
        Dilemma dilemma = Dilemma.builder()
                .sessionId(sessionId)
                .decisionId(1L)
                .dilemmaType(DilemmaType.ALT_PATH)
                .scenarioText("시나리오")
                .verificationGoal("목표")
                .fallback(false)
                .build();
        setId(dilemma, 5L);
        given(dilemmaRepository.findBySessionId(sessionId)).willReturn(Optional.of(dilemma));
        given(reportGenerator.generate(sessionId, ReportType.REPORT, EntryPoint.S05))
                .willReturn(reportResult(123L));

        ResolveResponse response = decisionService.resolve(sessionId, decisionId, new ResolveRequest(DecisionAction.APPROVE));

        verify(dilemmaGenerator, never()).generate(any(), any());
        verify(sessionStateService).transition(sessionId, SessionState.REPORT);
        assertThat(response.reportId()).isEqualTo("123");
    }

    @Test
    void resolve_revise_deletesDecisionAndCriteriaThenGoesToExploring() throws Exception {
        ProvisionalDecision decision = decision();
        given(provisionalDecisionRepository.findById(decisionId)).willReturn(Optional.of(decision));
        JudgmentCriterion criterion = JudgmentCriterion.builder()
                .sessionId(sessionId)
                .decisionId(decisionId)
                .category("VALUE")
                .content("연구 전문성")
                .source(CriterionSource.USER_STATED)
                .build();
        given(judgmentCriterionRepository.findByDecisionId(decisionId)).willReturn(List.of(criterion));

        ResolveResponse response = decisionService.resolve(sessionId, decisionId, new ResolveRequest(DecisionAction.REVISE));

        verify(judgmentCriterionRepository).deleteAll(List.of(criterion));
        verify(provisionalDecisionRepository).delete(decision);
        verify(sessionStateService).transition(sessionId, SessionState.EXPLORING);
        assertThat(response.state()).isEqualTo(SessionState.EXPLORING);
    }

    @Test
    void resolve_undecided_generatesUndecidedReportAndGoesToReport() throws Exception {
        given(provisionalDecisionRepository.findById(decisionId)).willReturn(Optional.of(decision()));
        given(reportGenerator.generate(sessionId, ReportType.UNDECIDED_REPORT, EntryPoint.S05))
                .willReturn(reportResult(77L));

        ResolveResponse response = decisionService.resolve(sessionId, decisionId, new ResolveRequest(DecisionAction.UNDECIDED));

        verify(sessionStateService).transition(sessionId, SessionState.REPORT);
        assertThat(response.reportId()).isEqualTo("77");
    }

    @Test
    void resolve_withDecisionFromAnotherSession_throwsNotFound() throws Exception {
        ProvisionalDecision decision = ProvisionalDecision.builder()
                .sessionId(UUID.randomUUID())
                .revisionNo(1)
                .summaryText("요약")
                .build();
        setId(decision, decisionId);
        given(provisionalDecisionRepository.findById(decisionId)).willReturn(Optional.of(decision));

        assertThatThrownBy(() -> decisionService.resolve(sessionId, decisionId, new ResolveRequest(DecisionAction.APPROVE)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    private ReportResult reportResult(Long reportId) {
        return new ReportResult(reportId, ReportType.REPORT, EntryPoint.S05, "요약", null, null, null, null, null,
                null, null, List.of(), "고지", LocalDateTime.now());
    }

    private void setId(Object entity, Long id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
