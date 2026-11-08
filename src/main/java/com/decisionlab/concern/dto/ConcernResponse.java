package com.decisionlab.concern.dto;

import com.decisionlab.session.entity.SessionState;

/** 명세서 4.3 고민 입력 응답. sessionId 외 ID는 숫자 문자열 (명세서 1장) */
public record ConcernResponse(SessionState state, String concernId) {

    public static ConcernResponse of(SessionState state, Long concernId) {
        return new ConcernResponse(state, String.valueOf(concernId));
    }
}
