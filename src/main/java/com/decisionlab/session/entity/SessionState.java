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
            // RECONSIDER: S07에서 유지 이유를 내기 전에 "생각이 바뀌었어요" (#11, 4.11)
            case REFLECTION -> EnumSet.of(REPORT, RECONSIDER);
            case RECONSIDER -> EnumSet.of(EXPLORING);
            // 리포트 화면의 "더 이야기해 볼래요" (#16 reopen, 4.16)
            case REPORT -> EnumSet.of(EXPLORING);
            // SAVE(PAUSED)는 EXPLORING에서만 가능하므로 이어하기는 항상 EXPLORING으로 돌아간다.
            case PAUSED -> EnumSet.of(EXPLORING);
        };
    }
}
