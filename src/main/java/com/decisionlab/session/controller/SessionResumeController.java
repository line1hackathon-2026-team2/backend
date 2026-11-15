package com.decisionlab.session.controller;

import com.decisionlab.session.dto.ResumeRequest;
import com.decisionlab.session.dto.ResumeResponse;
import com.decisionlab.session.service.SessionResumeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionResumeController {

    private final SessionResumeService sessionResumeService;

    public SessionResumeController(SessionResumeService sessionResumeService) {
        this.sessionResumeService = sessionResumeService;
    }

    /** #17 이어하기. 토큰 없이 호출한다 (SessionTokenFilter 예외 경로). */
    @PostMapping("/resume")
    public ResumeResponse resume(@Valid @RequestBody ResumeRequest request) {
        return sessionResumeService.resume(request);
    }
}
