package com.decisionlab.session.service;

import com.decisionlab.session.dto.ReopenResponse;
import com.decisionlab.session.entity.SessionState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SessionReopenService {

    private final SessionStateService sessionStateService;

    public SessionReopenService(SessionStateService sessionStateService) {
        this.sessionStateService = sessionStateService;
    }

    /**
     * #16 대화 재개 (S09, S09-U, S09-I). REPORT에서만 가능하다.
     * 대화 이력, 판단 기준, 리포트 행은 그대로 두고 상태만 EXPLORING으로 되돌린다.
     * 재개 후 리포트를 다시 만들면 C의 ReportGenerator가 같은 reportId로 덮어쓴다 (4.16).
     */
    @Transactional
    public ReopenResponse reopen(UUID sessionId) {
        sessionStateService.requireState(sessionId, SessionState.REPORT);
        sessionStateService.transition(sessionId, SessionState.EXPLORING);
        return ReopenResponse.exploring();
    }
}
