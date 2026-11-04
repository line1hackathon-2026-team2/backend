package com.decisionlab.session.service;

import com.decisionlab.session.entity.SessionState;

import java.util.UUID;

public interface SessionStateService {

    void requireState(UUID sessionId, SessionState... allowed);

    void transition(UUID sessionId, SessionState to);
}
