package com.decisionlab.persona.entity;

public enum PersonaCode {
    ACHIEVEMENT,
    STABILITY,
    EXPLORATION,
    DILEMMA_MANAGER;

    /** 사용자가 고를 수 있는 자아인지. DILEMMA_MANAGER는 딜레마 전용이라 선택 불가 (명세서 1장 Enum) */
    public boolean isSelectable() {
        return this != DILEMMA_MANAGER;
    }
}
