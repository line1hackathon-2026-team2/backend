package com.decisionlab.session.dto;

import com.decisionlab.global.time.ApiTime;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.entity.SessionState;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 명세서 4.1 세션 생성 응답 */
public record SessionCreateResponse(
        UUID sessionId,
        String sessionToken,
        SessionState state,
        OffsetDateTime expiresAt
) {

    public static SessionCreateResponse from(Session session) {
        return new SessionCreateResponse(
                session.getId(),
                session.getSessionToken(),
                session.getState(),
                ApiTime.toKst(session.getExpiresAt())
        );
    }
}
