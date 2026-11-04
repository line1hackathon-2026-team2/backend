package com.decisionlab.dilemma.dto;

import com.decisionlab.dilemma.entity.DilemmaType;

public record DilemmaResult(
        Long dilemmaId,
        DilemmaType dilemmaType,
        String scenarioText,
        String verificationGoal,
        boolean isFallback
) {
}
