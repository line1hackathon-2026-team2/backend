package com.decisionlab.chat.decision.service;

import com.decisionlab.ai.decision.DecisionExtractor;
import com.decisionlab.ai.dilemma.DilemmaGenerator;
import com.decisionlab.ai.report.ReportGenerator;
import com.decisionlab.chat.criterion.entity.JudgmentCriterion;
import com.decisionlab.chat.criterion.repository.JudgmentCriterionRepository;
import com.decisionlab.chat.decision.dto.DecisionDraftResponse;
import com.decisionlab.chat.decision.dto.DecisionDraftResult;
import com.decisionlab.chat.decision.dto.ResolveRequest;
import com.decisionlab.chat.decision.dto.ResolveResponse;
import com.decisionlab.chat.decision.entity.DecisionAction;
import com.decisionlab.chat.decision.entity.ProvisionalDecision;
import com.decisionlab.chat.decision.repository.ProvisionalDecisionRepository;
import com.decisionlab.dilemma.dto.DilemmaResult;
import com.decisionlab.dilemma.entity.DilemmaChoice;
import com.decisionlab.dilemma.repository.DilemmaRepository;
import com.decisionlab.dilemma.repository.DilemmaResponseRepository;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.report.dto.ReportResult;
import com.decisionlab.report.entity.EntryPoint;
import com.decisionlab.report.entity.ReportType;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.service.SessionStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DecisionService {

    private final SessionStateService sessionStateService;
    private final DecisionExtractor decisionExtractor;
    private final ProvisionalDecisionRepository provisionalDecisionRepository;
    private final JudgmentCriterionRepository judgmentCriterionRepository;
    private final DilemmaGenerator dilemmaGenerator;
    private final ReportGenerator reportGenerator;
    // C의 테이블은 읽기만 한다 (재검토 여부 판단용)
    private final DilemmaRepository dilemmaRepository;
    private final DilemmaResponseRepository dilemmaResponseRepository;

    /** #8 잠정 결정 요약 생성 (S04→S05, F04) */
    @Transactional
    public DecisionDraftResponse draft(UUID sessionId) {
        sessionStateService.requireState(sessionId, SessionState.EXPLORING);

        DecisionDraftResult result = decisionExtractor.extract(sessionId);
        int revisionNo = nextRevisionNo(sessionId);

        ProvisionalDecision decision = provisionalDecisionRepository.save(ProvisionalDecision.builder()
                .sessionId(sessionId)
                .revisionNo(revisionNo)
                .summaryText(result.summaryText())
                .decisionText(result.decisionText())
                .decisionTextSource(result.decisionTextSource())
                .coreValue(result.coreValue())
                .coreValueSource(result.coreValueSource())
                .reason(result.reason())
                .reasonSource(result.reasonSource())
                .acceptableCost(result.acceptableCost())
                .acceptableCostSource(result.acceptableCostSource())
                .uncheckedCondition(result.uncheckedCondition())
                .build());

        List<JudgmentCriterion> criteria = result.criteria().stream()
                .map(c -> judgmentCriterionRepository.save(JudgmentCriterion.builder()
                        .sessionId(sessionId)
                        .decisionId(decision.getId())
                        .category(c.category())
                        .content(c.content())
                        .source(c.source())
                        .build()))
                .toList();

        sessionStateService.transition(sessionId, SessionState.PROVISIONAL_DECISION);

        return DecisionDraftResponse.of(decision, criteria);
    }

    private int nextRevisionNo(UUID sessionId) {
        return provisionalDecisionRepository.findBySessionIdOrderByRevisionNoDesc(sessionId).stream()
                .findFirst()
                .map(d -> d.getRevisionNo() + 1)
                .orElse(1);
    }

    /** #10 잠정 결정 처리: 승인/수정/미결정 (S05) */
    @Transactional
    public ResolveResponse resolve(UUID sessionId, Long decisionId, ResolveRequest request) {
        sessionStateService.requireState(sessionId, SessionState.PROVISIONAL_DECISION);
        ProvisionalDecision decision = getOwnedDecision(sessionId, decisionId);

        return switch (request.action()) {
            case APPROVE -> handleApprove(sessionId, decision);
            case REVISE -> handleRevise(sessionId, decision);
            case UNDECIDED -> handleUndecided(sessionId, decision);
        };
    }

    private ResolveResponse handleApprove(UUID sessionId, ProvisionalDecision decision) {
        decision.resolve(DecisionAction.APPROVE);

        if (hasReconsidered(sessionId)) {
            // 재검토 후 APPROVE: 딜레마를 다시 만들지 않고 바로 REPORT (명세서 4.10)
            ReportResult report = reportGenerator.generate(sessionId, ReportType.REPORT, EntryPoint.S05);
            sessionStateService.transition(sessionId, SessionState.REPORT);
            return ResolveResponse.toReport(report.reportId());
        }

        DilemmaResult dilemma = dilemmaGenerator.generate(sessionId, decision.getId());
        sessionStateService.transition(sessionId, SessionState.DILEMMA);
        return ResolveResponse.toDilemma(dilemma);
    }

    private ResolveResponse handleRevise(UUID sessionId, ProvisionalDecision decision) {
        decision.resolve(DecisionAction.REVISE);
        // "해당 결정 요약만 폐기" (명세서 4.10) — 대화 이력은 그대로 둔다
        judgmentCriterionRepository.deleteAll(judgmentCriterionRepository.findByDecisionId(decision.getId()));
        provisionalDecisionRepository.delete(decision);

        sessionStateService.transition(sessionId, SessionState.EXPLORING);
        return ResolveResponse.toExploring();
    }

    private ResolveResponse handleUndecided(UUID sessionId, ProvisionalDecision decision) {
        decision.resolve(DecisionAction.UNDECIDED);

        ReportResult report = reportGenerator.generate(sessionId, ReportType.UNDECIDED_REPORT, EntryPoint.S05);
        sessionStateService.transition(sessionId, SessionState.REPORT);
        return ResolveResponse.toReport(report.reportId());
    }

    /**
     * 딜레마는 세션당 1개라 RECONSIDER는 자연히 1회만 가능하다 (명세서 4.11).
     * sessions 테이블에 별도 카운터 컬럼을 추가하는 대신, 세션의 딜레마에 RECONSIDER 응답이
     * 있었는지로 reconsiderCount >= 1 여부를 판단한다.
     */
    private boolean hasReconsidered(UUID sessionId) {
        return dilemmaRepository.findBySessionId(sessionId)
                .flatMap(dilemma -> dilemmaResponseRepository.findByDilemmaId(dilemma.getId()))
                .map(response -> response.getChoice() == DilemmaChoice.RECONSIDER)
                .orElse(false);
    }

    private ProvisionalDecision getOwnedDecision(UUID sessionId, Long decisionId) {
        ProvisionalDecision decision = provisionalDecisionRepository.findById(decisionId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "잠정 결정을 찾을 수 없습니다."));
        if (!decision.getSessionId().equals(sessionId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "잠정 결정을 찾을 수 없습니다.");
        }
        return decision;
    }
}
