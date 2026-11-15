package com.decisionlab.session.dto;

import com.decisionlab.session.entity.SessionState;

/** #16 응답: { "state": "EXPLORING" } */
public record ReopenResponse(SessionState state) {

    public static ReopenResponse exploring() {
        return new ReopenResponse(SessionState.EXPLORING);
    }
}
