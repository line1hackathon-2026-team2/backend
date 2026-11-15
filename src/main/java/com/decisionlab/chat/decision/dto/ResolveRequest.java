package com.decisionlab.chat.decision.dto;

import com.decisionlab.chat.decision.entity.DecisionAction;
import jakarta.validation.constraints.NotNull;

/** 명세서 4.10 잠정 결정 처리 요청 */
public record ResolveRequest(@NotNull DecisionAction action) {
}
