package com.decisionlab.chat.decision.dto;

import com.decisionlab.chat.criterion.entity.CriterionSource;
import com.decisionlab.chat.decision.entity.FieldSource;

import java.util.List;

/**
 * Decision Extractor 출력. 대화에서 근거를 찾지 못한 값은 "unknown".
 * uncheckedCondition은 출처를 따로 두지 않는다(정의상 항상 NEEDS_CHECK).
 */
public record DecisionDraftResult(
        String summaryText,
        String decisionText,
        FieldSource decisionTextSource,
        String coreValue,
        FieldSource coreValueSource,
        String reason,
        FieldSource reasonSource,
        String acceptableCost,
        FieldSource acceptableCostSource,
        String uncheckedCondition,
        List<CriterionDraft> criteria
) {

    public record CriterionDraft(String category, String content, CriterionSource source) {
    }
}
