package com.decisionlab.concern.controller;

import com.decisionlab.concern.dto.ConcernRequest;
import com.decisionlab.concern.dto.ConcernResponse;
import com.decisionlab.concern.service.ConcernService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}")
public class ConcernController {

    private final ConcernService concernService;

    public ConcernController(ConcernService concernService) {
        this.concernService = concernService;
    }

    /** #3 고민 입력 (덮어쓰기 가능). 경로의 sessionId와 토큰의 세션이 같은지는 SessionTokenFilter가 확인한다. */
    @PutMapping("/concern")
    public ConcernResponse saveConcern(@PathVariable UUID sessionId,
                                       @Valid @RequestBody ConcernRequest request) {
        return concernService.saveConcern(sessionId, request);
    }
}
