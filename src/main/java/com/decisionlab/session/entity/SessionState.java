package com.decisionlab.session.entity;

import java.util.EnumSet;
import java.util.Set;

public enum SessionState {
    CREATED,
    EXPLORING,
    PROVISIONAL_DECISION,
    DILEMMA,
    REFLECTION,
    RECONSIDER,
    REPORT,
    PAUSED;

    // 같은 상태로의 전이는 CREATED, EXPLORING만 허용.
    public boolean canTransitionTo(SessionState next) {
        return allowedNext().contains(next);
    }

    private Set<SessionState> allowedNext() {
        return switch (this) {
            case CREATED -> EnumSet.of(CREATED, EXPLORING);
            case EXPLORING -> EnumSet.of(EXPLORING, PROVISIONAL_DECISION, REPORT, PAUSED);
            case PROVISIONAL_DECISION -> EnumSet.of(DILEMMA, EXPLORING, REPORT);
            case DILEMMA -> EnumSet.of(REFLECTION, RECONSIDER, REPORT);
            case REFLECTION -> EnumSet.of(REPORT);
            case RECONSIDER -> EnumSet.of(EXPLORING);
            case REPORT -> EnumSet.noneOf(SessionState.class);
            // SAVE(PAUSED)는 EXPLORING에서만 가능하므로 이어하기는 항상 EXPLORING으로 돌아간다.
            case PAUSED -> EnumSet.of(EXPLORING);
        };
    }
}
