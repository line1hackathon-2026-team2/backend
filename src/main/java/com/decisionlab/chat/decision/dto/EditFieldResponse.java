package com.decisionlab.chat.decision.dto;

import com.decisionlab.chat.decision.dto.DecisionDraftResponse.FieldSources;
import com.decisionlab.chat.decision.dto.DecisionDraftResponse.Fields;
import com.decisionlab.chat.decision.entity.ProvisionalDecision;

/** 명세서 4.9 잠정 결정 필드 수정 응답 (P1). summaryText는 재생성하지 않아 포함하지 않는다. */
public record EditFieldResponse(
        String decisionId,
        Integer revisionNo,
        Fields fields,
        FieldSources fieldSources
) {

    public static EditFieldResponse from(ProvisionalDecision decision) {
        return new EditFieldResponse(
                String.valueOf(decision.getId()),
                decision.getRevisionNo(),
                new Fields(
                        decision.getDecisionText(),
                        decision.getCoreValue(),
                        decision.getReason(),
                        decision.getAcceptableCost(),
                        decision.getUncheckedCondition()
                ),
                FieldSources.from(decision)
        );
    }
}
