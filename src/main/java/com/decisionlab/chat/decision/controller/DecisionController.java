package com.decisionlab.chat.decision.controller;

import com.decisionlab.chat.decision.dto.DecisionDraftResponse;
import com.decisionlab.chat.decision.service.DecisionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/decision")
public class DecisionController {

    private final DecisionService decisionService;

    public DecisionController(DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    /** #8 잠정 결정 요약 생성 (S04→S05) */
    @PostMapping("/draft")
    public DecisionDraftResponse draft(@PathVariable UUID sessionId) {
        return decisionService.draft(sessionId);
    }
}
