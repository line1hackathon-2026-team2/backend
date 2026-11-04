package com.decisionlab.global.auth;

import java.util.UUID;

public final class SessionContext {

    private static final ThreadLocal<UUID> CURRENT_SESSION_ID = new ThreadLocal<>();

    private SessionContext() {
    }

    public static void setSessionId(UUID sessionId) {
        CURRENT_SESSION_ID.set(sessionId);
    }

    public static UUID getSessionId() {
        return CURRENT_SESSION_ID.get();
    }

    public static void clear() {
        CURRENT_SESSION_ID.remove();
    }
}
