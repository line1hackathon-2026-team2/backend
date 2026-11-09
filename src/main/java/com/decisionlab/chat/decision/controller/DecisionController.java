package com.decisionlab.chat.decision.controller;

import com.decisionlab.chat.decision.dto.EditFieldRequest;
import com.decisionlab.chat.decision.dto.EditFieldResponse;
import com.decisionlab.chat.decision.service.DecisionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    /** #9 잠정 결정 필드 수정 (S05, P1) */
    @PatchMapping("/{decisionId}")
    public EditFieldResponse editField(@PathVariable UUID sessionId, @PathVariable Long decisionId,
                                        @Valid @RequestBody EditFieldRequest request) {
        return decisionService.editField(sessionId, decisionId, request);
    }
}
