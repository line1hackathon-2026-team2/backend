package com.decisionlab.chat.decision.service;

import com.decisionlab.ai.decision.DecisionExtractor;
import com.decisionlab.chat.criterion.entity.JudgmentCriterion;
import com.decisionlab.chat.criterion.repository.JudgmentCriterionRepository;
import com.decisionlab.chat.decision.dto.DecisionDraftResponse;
import com.decisionlab.chat.decision.dto.DecisionDraftResult;
import com.decisionlab.chat.decision.entity.ProvisionalDecision;
import com.decisionlab.chat.decision.repository.ProvisionalDecisionRepository;
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
                .coreValue(result.coreValue())
                .reason(result.reason())
                .acceptableCost(result.acceptableCost())
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
}
