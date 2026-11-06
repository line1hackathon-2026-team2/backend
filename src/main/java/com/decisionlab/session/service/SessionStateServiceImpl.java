package com.decisionlab.session.service;

import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;

/**
 * 사용법 (B, C 공통)
 * <pre>
 * sessionStateService.requireState(sessionId, SessionState.PROVISIONAL_DECISION); // 맨 앞에서 검증
 * ... 비즈니스 로직 (LLM 호출 등) ...
 * sessionStateService.transition(sessionId, SessionState.DILEMMA);               // 마지막에 전이
 * </pre>
 * transition은 호출자의 트랜잭션에 참여하므로, 이후 로직에서 예외가 나면 상태 변경도 함께 롤백된다.
 */

@Service
@RequiredArgsConstructor
public class SessionStateServiceImpl implements SessionStateService {

    private final SessionRepository sessionRepository;

    @Override
    @Transactional(readOnly = true)
    public void requireState(UUID sessionId, SessionState... allowed) {
        Session session = getSession(sessionId);
        SessionState current = session.getState();
        if (Arrays.stream(allowed).noneMatch(current::equals)) {
            throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION,
                    "현재 상태(" + current + ")에서는 이 요청을 처리할 수 없습니다.", current.name());
        }
    }

    @Override
    @Transactional
    public void transition(UUID sessionId, SessionState to) {
        Session session = getSession(sessionId);
        SessionState current = session.getState();

        if (!current.canTransitionTo(to)) {
            throw invalidTransition(current, to);
        }
        if (current == to) {
            return;
        }

        // 읽은 뒤 다른 요청이 먼저 상태를 바꿨다면 0이 반환된다 -> 409
        int updated = sessionRepository.updateStateIfMatches(sessionId, current, to);
        if (updated == 0) {
            throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION,
                    "다른 요청이 먼저 처리되어 상태가 바뀌었습니다. 세션을 다시 조회해 주세요.", current.name());
        }
        // 영속성 컨텍스트의 엔티티도 DB와 같은 값으로 맞춘다 (updated_at 갱신 포함)
        session.transitionTo(to);
    }

    private Session getSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "세션을 찾을 수 없습니다."));
    }

    private ApiException invalidTransition(SessionState from, SessionState to) {
        return new ApiException(ErrorCode.INVALID_STATE_TRANSITION,
                "현재 상태(" + from + ")에서 " + to + "(으)로 전환할 수 없습니다.", from.name());
    }
}
