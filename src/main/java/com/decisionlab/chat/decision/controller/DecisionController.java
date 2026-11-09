package com.decisionlab.chat.decision.controller;

import com.decisionlab.chat.decision.dto.ResolveRequest;
import com.decisionlab.chat.decision.dto.ResolveResponse;
import com.decisionlab.chat.decision.service.DecisionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /** #10 잠정 결정 처리: 승인/수정/미결정 (S05) */
    @PostMapping("/{decisionId}/resolve")
    public ResolveResponse resolve(@PathVariable UUID sessionId, @PathVariable Long decisionId,
                                    @Valid @RequestBody ResolveRequest request) {
        return decisionService.resolve(sessionId, decisionId, request);
    }
}
