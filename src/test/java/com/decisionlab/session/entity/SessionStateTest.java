package com.decisionlab.session.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SessionStateTest {

    @ParameterizedTest(name = "{0} -> {1} 허용")
    @CsvSource({
            "CREATED, CREATED",
            "CREATED, EXPLORING",
            "EXPLORING, EXPLORING",
            "EXPLORING, PROVISIONAL_DECISION",
            "EXPLORING, REPORT",
            "EXPLORING, PAUSED",
            "PROVISIONAL_DECISION, DILEMMA",
            "PROVISIONAL_DECISION, EXPLORING",
            "PROVISIONAL_DECISION, REPORT",
            "DILEMMA, REFLECTION",
            "DILEMMA, RECONSIDER",
            "DILEMMA, REPORT",
            "REFLECTION, REPORT",
            "RECONSIDER, EXPLORING",
            "PAUSED, EXPLORING"
    })
    void allowedTransitions(SessionState from, SessionState to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} 금지")
    @CsvSource({
            "CREATED, PROVISIONAL_DECISION",
            "EXPLORING, DILEMMA",
            "PROVISIONAL_DECISION, PROVISIONAL_DECISION",
            "DILEMMA, DILEMMA",
            "REFLECTION, EXPLORING",
            "RECONSIDER, REPORT",
            "PAUSED, REPORT"
    })
    void forbiddenTransitions(SessionState from, SessionState to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    void reportIsTerminal() {
        for (SessionState next : SessionState.values()) {
            assertThat(SessionState.REPORT.canTransitionTo(next)).isFalse();
        }
    }
}
