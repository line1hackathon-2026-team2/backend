package com.decisionlab.report.service;

import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.report.dto.ReportResponse;
import com.decisionlab.report.entity.Report;
import com.decisionlab.report.entity.ReportAction;
import com.decisionlab.report.repository.ReportActionRepository;
import com.decisionlab.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportActionRepository reportActionRepository;

    /** #14 세션의 의사결정 리포트와 다음 행동 목록을 조회한다 (명세서 4.14). */
    @Transactional(readOnly = true)
    public ReportResponse getReport(UUID sessionId) {
        Report report = reportRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "리포트를 찾을 수 없습니다."));

        return toResponse(report);
    }

    /** #15 리포트 ID로 의사결정 리포트와 다음 행동 목록을 공개 조회한다 (명세서 4.15). */
    @Transactional(readOnly = true)
    public ReportResponse getReportById(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "리포트를 찾을 수 없습니다."));

        return toResponse(report);
    }

    private ReportResponse toResponse(Report report) {
        List<ReportAction> actions = reportActionRepository.findByReportIdOrderBySeqAsc(report.getId());
        return ReportResponse.from(report, actions);
    }
}
