package com.decisionlab.chat.decision.dto;

import com.decisionlab.chat.criterion.entity.CriterionSource;
import com.decisionlab.chat.criterion.entity.JudgmentCriterion;
import com.decisionlab.chat.decision.entity.FieldSource;
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
        FieldSources fieldSources,
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

    /** uncheckedCondition은 정의상 항상 NEEDS_CHECK. */
    public record FieldSources(
            FieldSource decisionText,
            FieldSource coreValue,
            FieldSource reason,
            FieldSource acceptableCost,
            FieldSource uncheckedCondition
    ) {

        public static FieldSources from(ProvisionalDecision decision) {
            return new FieldSources(
                    decision.getDecisionTextSource(),
                    decision.getCoreValueSource(),
                    decision.getReasonSource(),
                    decision.getAcceptableCostSource(),
                    FieldSource.NEEDS_CHECK
            );
        }
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
                FieldSources.from(decision),
                criteria.stream().map(CriterionItem::from).toList()
        );
    }
}
