package com.decisionlab.chat.decision.dto;

import com.decisionlab.chat.criterion.entity.CriterionSource;

import java.util.List;

/** Decision Extractor 출력. 대화에서 근거를 찾지 못한 값은 "unknown". */
public record DecisionDraftResult(
        String summaryText,
        String decisionText,
        String coreValue,
        String reason,
        String acceptableCost,
        String uncheckedCondition,
        List<CriterionDraft> criteria
) {

    public record CriterionDraft(String category, String content, CriterionSource source) {
    }
}
