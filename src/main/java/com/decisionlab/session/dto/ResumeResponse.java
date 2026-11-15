package com.decisionlab.session.dto;

import com.decisionlab.session.entity.SessionState;

import java.util.UUID;

/**
 * #17 이어하기 응답. 프론트는 새 sessionToken으로 교체해 저장한다.
 * 팀 결정에 따라 state는 PAUSED가 아니라 바로 EXPLORING이다 (SAVE는 EXPLORING에서만 가능하므로 "이전 상태"와 같다).
 */
public record ResumeResponse(UUID sessionId, String sessionToken, SessionState state) {
}
