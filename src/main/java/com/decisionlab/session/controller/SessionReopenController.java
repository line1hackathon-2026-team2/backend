package com.decisionlab.session.controller;

import com.decisionlab.session.dto.ReopenResponse;
import com.decisionlab.session.service.SessionReopenService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}")
public class SessionReopenController {

    private final SessionReopenService sessionReopenService;

    public SessionReopenController(SessionReopenService sessionReopenService) {
        this.sessionReopenService = sessionReopenService;
    }

    /** #16 대화 재개. 요청 본문 없음. 경로의 sessionId와 토큰의 세션이 같은지는 SessionTokenFilter가 확인한다. */
    @PostMapping("/reopen")
    public ReopenResponse reopen(@PathVariable UUID sessionId) {
        return sessionReopenService.reopen(sessionId);
    }
}
