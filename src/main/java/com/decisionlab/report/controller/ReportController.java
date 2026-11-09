package com.decisionlab.report.controller;

import com.decisionlab.report.dto.ReportResponse;
import com.decisionlab.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** #13 리포트 조회 (S09). 경로의 sessionId와 토큰의 세션이 같은지는 SessionTokenFilter가 확인한다. */
    @Operation(summary = "#13 리포트 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리포트 조회 성공"),
            @ApiResponse(responseCode = "401", description = "세션 토큰 없음 또는 불일치"),
            @ApiResponse(responseCode = "403", description = "다른 세션 접근"),
            @ApiResponse(responseCode = "404", description = "리포트 없음"),
            @ApiResponse(responseCode = "410", description = "세션 만료")
    })
    @GetMapping
    public ReportResponse getReport(@PathVariable UUID sessionId) {
        return reportService.getReport(sessionId);
    }
}
