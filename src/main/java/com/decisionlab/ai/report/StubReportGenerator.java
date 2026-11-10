package com.decisionlab.ai.report;

import com.decisionlab.report.dto.ReportResult;
import com.decisionlab.report.entity.EntryPoint;
import com.decisionlab.report.entity.Report;
import com.decisionlab.report.entity.ReportAction;
import com.decisionlab.report.entity.ReportType;
import com.decisionlab.report.repository.ReportActionRepository;
import com.decisionlab.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StubReportGenerator implements ReportGenerator {

    private static final String DEFAULT_DISCLAIMER = "이 리포트는 선택의 정답을 평가하지 않습니다.";

    private final ReportRepository reportRepository;
    private final ReportActionRepository reportActionRepository;

    @Override
    @Transactional
    public ReportResult generate(UUID sessionId, ReportType type, EntryPoint entry) {
        Report report = reportRepository.findBySessionId(sessionId)
                .orElseGet(() -> createStubReport(sessionId, type, entry));

        List<ReportAction> actions = reportActionRepository.findByReportIdOrderBySeqAsc(report.getId());
        if (actions.isEmpty()) {
            actions = createStubActions(report.getId(), type);
        }

        List<ReportResult.NextAction> nextActions = actions.stream()
                .map(action -> new ReportResult.NextAction(action.getSeq(), action.getContent()))
                .toList();

        LocalDateTime generatedAt = report.getCreatedAt() != null ? report.getCreatedAt() : LocalDateTime.now();

        return new ReportResult(
                report.getId(),
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
                nextActions,
                report.getDisclaimer(),
                generatedAt
        );
    }

    private Report createStubReport(UUID sessionId, ReportType type, EntryPoint entry) {
        Report report = switch (type) {
            case REPORT -> Report.builder()
                    .sessionId(sessionId)
                    .reportType(ReportType.REPORT)
                    .entryPoint(entry)
                    .concernSummary("취업과 대학원 진학 사이의 진로 고민")
                    .initialView("대학원에 진학해 연구 역량을 쌓고 싶다")
                    .initialViewReason("전문성을 키우고 싶다")
                    .revisedView("대학원 진학을 유지하되, 연구실 인턴으로 먼저 적합성을 확인한다")
                    .revisedViewReason("연구실 인턴으로 적합성부터 확인")
                    .guidingValues("연구 전문성")
                    .shakingSituation("희망 기업의 연구개발 직무 제안")
                    .shakingSituationDetail("대학원 이외의 경로에서도 연구 역량을 쌓을 수 있다는 가능성을 고려하게 되었어요")
                    .clarifiedCriteria("학위 자체보다 체계적인 연구 방법 습득이 중요함")
                    .undecidedReason(null)
                    .missingInfo("실제 연구실 생활 적합성")
                    .disclaimer(DEFAULT_DISCLAIMER)
                    .build();

            case UNDECIDED_REPORT -> Report.builder()
                    .sessionId(sessionId)
                    .reportType(ReportType.UNDECIDED_REPORT)
                    .entryPoint(entry)
                    .concernSummary("취업과 대학원 진학 중 현재 비교 중인 선택지")
                    .initialView(null)
                    .initialViewReason(null)
                    .revisedView(null)
                    .revisedViewReason(null)
                    .guidingValues("연구 전문성과 경제적 안정성")
                    .shakingSituation(entry == EntryPoint.S06 ? "희망 기업의 연구개발 직무 제안" : null)
                    .shakingSituationDetail(entry == EntryPoint.S06
                            ? "대학원 이외의 경로에서도 연구 역량을 쌓을 수 있다는 가능성을 고려하게 되었어요"
                            : null)
                    .clarifiedCriteria(null)
                    .undecidedReason("실제 연구 환경과 현업 R&D 직무의 차이를 아직 충분히 비교하지 못함")
                    .missingInfo("현업 연구개발 직무의 실제 업무 범위와 성장 경로")
                    .disclaimer(DEFAULT_DISCLAIMER)
                    .build();

            case INCOMPLETE_REPORT -> Report.builder()
                    .sessionId(sessionId)
                    .reportType(ReportType.INCOMPLETE_REPORT)
                    .entryPoint(entry)
                    .concernSummary("취업과 대학원 진학 사이의 초기 고민 탐색")
                    .initialView(null)
                    .initialViewReason(null)
                    .revisedView(null)
                    .revisedViewReason(null)
                    .guidingValues("현재까지 확인된 핵심 기준: 장기적 전문성")
                    .shakingSituation(null)
                    .shakingSituationDetail(null)
                    .clarifiedCriteria(null)
                    .undecidedReason(null)
                    .missingInfo("감수할 수 있는 기회비용과 구체적인 대안 조건")
                    .disclaimer(DEFAULT_DISCLAIMER)
                    .build();
        };

        return reportRepository.save(report);
    }

    private List<ReportAction> createStubActions(Long reportId, ReportType type) {
        List<ReportAction> actions = switch (type) {
            case REPORT -> List.of(
                    ReportAction.builder().reportId(reportId).seq(1).content("희망 연구실 학부 인턴 프로그램 지원 일정 확인하기").build(),
                    ReportAction.builder().reportId(reportId).seq(2).content("대학원 재학생 선배에게 연구실 일상과 진로 면담 요청하기").build()
            );
            case UNDECIDED_REPORT -> List.of(
                    ReportAction.builder().reportId(reportId).seq(1).content("관심 기업의 석사 우대 여부 및 학사 R&D 직무 JD 비교하기").build(),
                    ReportAction.builder().reportId(reportId).seq(2).content("결정 시한과 최소 판단 기준 1가지를 메모로 정리하기").build()
            );
            case INCOMPLETE_REPORT -> List.of(
                    ReportAction.builder().reportId(reportId).seq(1).content("두 선택지에서 가장 포기하기 어려운 가치 1가지 적어보기").build()
            );
        };

        return reportActionRepository.saveAll(actions);
    }
}
