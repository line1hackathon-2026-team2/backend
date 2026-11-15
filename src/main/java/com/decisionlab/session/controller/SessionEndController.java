package com.decisionlab.session.controller;

import com.decisionlab.session.dto.EndRequest;
import com.decisionlab.session.dto.EndResponse;
import com.decisionlab.session.service.SessionEndService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}")
public class SessionEndController {

    private final SessionEndService sessionEndService;

    public SessionEndController(SessionEndService sessionEndService) {
        this.sessionEndService = sessionEndService;
    }

    /** #13 그만 대화하기. 경로의 sessionId와 토큰의 세션이 같은지는 SessionTokenFilter가 확인한다. */
    @PostMapping("/end")
    public EndResponse end(@PathVariable UUID sessionId, @Valid @RequestBody EndRequest request) {
        return sessionEndService.end(sessionId, request);
    }
}
