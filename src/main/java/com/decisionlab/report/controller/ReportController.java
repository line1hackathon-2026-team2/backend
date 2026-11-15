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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** #14 리포트 조회 (S09). 경로의 sessionId와 토큰의 세션이 같은지는 SessionTokenFilter가 확인한다. */
    @Operation(summary = "#14 리포트 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리포트 조회 성공"),
            @ApiResponse(responseCode = "401", description = "세션 토큰 없음 또는 불일치"),
            @ApiResponse(responseCode = "403", description = "다른 세션 접근"),
            @ApiResponse(responseCode = "404", description = "리포트 없음"),
            @ApiResponse(responseCode = "410", description = "세션 만료")
    })
    @GetMapping("/sessions/{sessionId}/report")
    public ReportResponse getReport(@PathVariable UUID sessionId) {
        return reportService.getReport(sessionId);
    }

    /** #15 리포트 공개 조회 (S09, S09-U, S09-I, S10). X-Session-Token 없이 reportId로 조회한다. */
    @Operation(summary = "#15 리포트 공개 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리포트 공개 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 reportId 형식"),
            @ApiResponse(responseCode = "404", description = "리포트 없음")
    })
    @GetMapping("/reports/{reportId}")
    public ReportResponse getReportById(@PathVariable Long reportId) {
        return reportService.getReportById(reportId);
    }
}
