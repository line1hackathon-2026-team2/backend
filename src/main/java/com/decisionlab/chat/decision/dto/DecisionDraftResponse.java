package com.decisionlab.chat.decision.dto;

import com.decisionlab.chat.criterion.entity.CriterionSource;
import com.decisionlab.chat.criterion.entity.JudgmentCriterion;
import com.decisionlab.chat.decision.entity.ProvisionalDecision;
import com.decisionlab.session.entity.SessionState;

import java.util.List;

/** 명세서 4.8 잠정 결정 요약 생성 응답 */
public record DecisionDraftResponse(
        SessionState state,
        String decisionId,
        Integer revisionNo,
        String summaryText,
        Fields fields,
        List<CriterionItem> criteria
) {

    public record Fields(
            String decisionText,
            String coreValue,
            String reason,
            String acceptableCost,
            String uncheckedCondition
    ) {
    }

    public record CriterionItem(String criterionId, String category, String content, CriterionSource source) {

        public static CriterionItem from(JudgmentCriterion criterion) {
            return new CriterionItem(
                    String.valueOf(criterion.getId()),
                    criterion.getCategory(),
                    criterion.getContent(),
                    criterion.getSource()
            );
        }
    }

    public static DecisionDraftResponse of(ProvisionalDecision decision, List<JudgmentCriterion> criteria) {
        return new DecisionDraftResponse(
                SessionState.PROVISIONAL_DECISION,
                String.valueOf(decision.getId()),
                decision.getRevisionNo(),
                decision.getSummaryText(),
                new Fields(
                        decision.getDecisionText(),
                        decision.getCoreValue(),
                        decision.getReason(),
                        decision.getAcceptableCost(),
                        decision.getUncheckedCondition()
                ),
                criteria.stream().map(CriterionItem::from).toList()
        );
    }
}
