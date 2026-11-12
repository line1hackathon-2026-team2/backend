package com.decisionlab.session.controller;

import com.decisionlab.session.dto.SessionDetailResponse;
import com.decisionlab.session.service.SessionQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}")
public class SessionQueryController {

    private final SessionQueryService sessionQueryService;

    public SessionQueryController(SessionQueryService sessionQueryService) {
        this.sessionQueryService = sessionQueryService;
    }

    /** #2 세션 조회 (새로고침 복원). 경로의 sessionId와 토큰의 세션이 같은지는 SessionTokenFilter가 확인한다. */
    @GetMapping
    public SessionDetailResponse getSession(@PathVariable UUID sessionId) {
        return sessionQueryService.getSession(sessionId);
    }
}
