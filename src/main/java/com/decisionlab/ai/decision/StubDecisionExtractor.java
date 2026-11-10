package com.decisionlab.ai.decision;

import com.decisionlab.chat.criterion.entity.CriterionSource;
import com.decisionlab.chat.decision.dto.DecisionDraftResult;
import com.decisionlab.chat.decision.dto.DecisionDraftResult.CriterionDraft;
import com.decisionlab.chat.decision.entity.FieldSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class StubDecisionExtractor implements DecisionExtractor {

    @Override
    public DecisionDraftResult extract(UUID sessionId) {
        return new DecisionDraftResult(
                "현재는 대학원 진학에 조금 더 마음이 기울어 있고, 가장 중요한 이유는 연구 역량을 쌓고 싶기 때문으로 이해했어요.",
                "대학원 진학",
                FieldSource.USER_STATED,
                "연구 전문성",
                FieldSource.AI_INFERRED,
                "장기적으로 연구개발 분야에서 일하고 싶음",
                FieldSource.USER_STATED,
                "취업 시점의 지연",
                FieldSource.AI_INFERRED,
                "실제 대학원 연구 경험",
                List.of(new CriterionDraft("VALUE", "연구 전문성", CriterionSource.USER_STATED))
        );
    }
}
