package com.decisionlab.report.dto;

import com.decisionlab.global.time.ApiTime;
import com.decisionlab.report.entity.EntryPoint;
import com.decisionlab.report.entity.Report;
import com.decisionlab.report.entity.ReportAction;
import com.decisionlab.report.entity.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/** 명세서 4.14 리포트 조회 응답 */
public record ReportResponse(
        @Schema(description = "리포트 ID (숫자 문자열)", example = "1")
        String reportId,

        @Schema(description = "리포트 유형", example = "REPORT")
        ReportType reportType,

        @Schema(description = "리포트 생성 진입 지점", example = "S07")
        EntryPoint entryPoint,

        @Schema(description = "고민 요약", example = "취업과 대학원 진학 사이의 고민")
        String concernSummary,

        @Schema(description = "초기 관점 (REPORT만 존재, 나머지는 null)", example = "대학원에 가고 싶다")
        String initialView,

        @Schema(description = "초기 관점 이유 (REPORT만 존재, 나머지는 null)", example = "전문성을 키우고 싶다")
        String initialViewReason,

        @Schema(description = "수정/유지된 최종 관점 (REPORT만 존재, 나머지는 null)",
                example = "대학원 진학을 유지하되, 연구실 인턴으로 먼저 적합성을 확인한다")
        String revisedView,

        @Schema(description = "최종 관점 이유 (REPORT만 존재, 나머지는 null)",
                example = "연구실 인턴으로 적합성부터 확인")
        String revisedViewReason,

        @Schema(description = "핵심 판단 가치/기준", example = "연구 전문성")
        String guidingValues,

        @Schema(description = "흔들렸던 가상 딜레마 상황", example = "희망 기업의 연구개발 직무 제안")
        String shakingSituation,

        @Schema(description = "흔들렸던 상황 상세 설명",
                example = "대학원 이외의 경로에서도 연구 역량을 쌓을 수 있다는 가능성을 고려하게 되었어요")
        String shakingSituationDetail,

        @Schema(description = "명확해진 판단 기준 (REPORT만 존재, 나머지는 null)",
                example = "학위보다 체계적인 연구 방법 습득이 중요함")
        String clarifiedCriteria,

        @Schema(description = "미결정 이유 (UNDECIDED_REPORT만 존재, 나머지는 null)", nullable = true)
        String undecidedReason,

        @Schema(description = "아직 확인하지 못한 정보", example = "실제 연구실 생활 적합성")
        String missingInfo,

        @Schema(description = "다음 실행 행동 목록")
        List<NextActionItem> nextActions,

        @Schema(description = "고지 문구", example = "이 리포트는 선택의 정답을 평가하지 않습니다.")
        String disclaimer,

        @Schema(description = "리포트 생성 시각 (ISO 8601 KST)", example = "2026-11-02T21:30:00+09:00")
        OffsetDateTime generatedAt
) {

    public static ReportResponse from(Report report, List<ReportAction> actions) {
        List<NextActionItem> actionItems = actions == null
                ? List.of()
                : actions.stream().map(NextActionItem::from).toList();

        LocalDateTime timestamp = report.getUpdatedAt() != null ? report.getUpdatedAt() : report.getCreatedAt();

        return new ReportResponse(
                report.getId() != null ? String.valueOf(report.getId()) : null,
                report.getReportType(),
                report.getEntryPoint(),
                report.getConcernSummary(),
                report.getInitialView(),
                report.getInitialViewReason(),
                report.getRevisedView(),
                report.getRevisedViewReason(),
                report.getGuidingValues(),
                report.getShakingSituation(),
                report.getShakingSituationDetail(),
                report.getClarifiedCriteria(),
                report.getUndecidedReason(),
                report.getMissingInfo(),
                actionItems,
                report.getDisclaimer(),
                ApiTime.toKst(timestamp)
        );
    }

    public record NextActionItem(
            @Schema(description = "행동 순서", example = "1")
            int seq,

            @Schema(description = "실행 행동 내용", example = "연구실 인턴 지원하기")
            String content
    ) {
        public static NextActionItem from(ReportAction action) {
            return new NextActionItem(action.getSeq(), action.getContent());
        }
    }
}
