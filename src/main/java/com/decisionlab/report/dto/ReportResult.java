package com.decisionlab.report.dto;

import com.decisionlab.report.entity.EntryPoint;
import com.decisionlab.report.entity.ReportType;

import java.time.LocalDateTime;
import java.util.List;

public record ReportResult(
        Long reportId,
        ReportType reportType,
        EntryPoint entryPoint,
        String concernSummary,
        String initialView,
        String revisedView,
        String guidingValues,
        String shakingSituation,
        String clarifiedCriteria,
        String undecidedReason,
        String missingInfo,
        List<NextAction> nextActions,
        String disclaimer,
        LocalDateTime generatedAt
) {

    public record NextAction(int seq, String content) {
    }
}
