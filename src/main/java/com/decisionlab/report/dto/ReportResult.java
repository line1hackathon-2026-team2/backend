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
        String initialViewReason,
        String revisedView,
        String revisedViewReason,
        String guidingValues,
        String shakingSituation,
        String shakingSituationDetail,
        String clarifiedCriteria,
        String undecidedReason,
        String missingInfo,
        List<NextAction> nextActions,
        String disclaimer,
        LocalDateTime generatedAt
) {

    public ReportResult(Long reportId, ReportType reportType, EntryPoint entryPoint, String concernSummary,
                        String initialView, String revisedView, String guidingValues, String shakingSituation,
                        String clarifiedCriteria, String undecidedReason, String missingInfo,
                        List<NextAction> nextActions, String disclaimer, LocalDateTime generatedAt) {
        this(reportId, reportType, entryPoint, concernSummary,
                initialView, null, revisedView, null,
                guidingValues, shakingSituation, null,
                clarifiedCriteria, undecidedReason, missingInfo,
                nextActions, disclaimer, generatedAt);
    }

    public record NextAction(int seq, String content) {
    }
}
