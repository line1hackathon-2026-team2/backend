package com.decisionlab.dilemma.dto;

import com.decisionlab.dilemma.entity.DilemmaType;

public record DilemmaResult(
        Long dilemmaId,
        DilemmaType dilemmaType,
        String title,
        String scenarioText,
        String verificationGoal,
        boolean isFallback
) {

    public DilemmaResult(Long dilemmaId, DilemmaType dilemmaType, String scenarioText,
                         String verificationGoal, boolean isFallback) {
        this(dilemmaId, dilemmaType, null, scenarioText, verificationGoal, isFallback);
    }
}
