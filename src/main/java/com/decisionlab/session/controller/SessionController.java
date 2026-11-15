package com.decisionlab.session.controller;

import com.decisionlab.session.dto.SessionCreateResponse;
import com.decisionlab.session.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /** #1 세션 생성 + 토큰 발급 (→ CREATED). 토큰 없이 호출 가능. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionCreateResponse createSession() {
        return sessionService.createSession();
    }
}
