package com.decisionlab.report.service;

import com.decisionlab.global.entity.BaseTimeEntity;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.report.dto.ReportResponse;
import com.decisionlab.report.entity.EntryPoint;
import com.decisionlab.report.entity.Report;
import com.decisionlab.report.entity.ReportAction;
import com.decisionlab.report.entity.ReportType;
import com.decisionlab.report.repository.ReportActionRepository;
import com.decisionlab.report.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    ReportRepository reportRepository;

    @Mock
    ReportActionRepository reportActionRepository;

    @InjectMocks
    ReportService reportService;

    private final UUID sessionId = UUID.randomUUID();

    @Test
    void 리포트와_행동_목록을_조회해_명세서_형식으로_반환한다() {
        Report report = Report.builder()
                .sessionId(sessionId)
                .reportType(ReportType.REPORT)
                .entryPoint(EntryPoint.S07)
                .concernSummary("취업과 대학원 진학 사이의 고민")
                .initialView("대학원에 가고 싶다")
                .initialViewReason("전문성을 키우고 싶다")
                .revisedView("대학원 진학을 유지하되, 연구실 인턴으로 먼저 적합성을 확인한다")
                .revisedViewReason("연구실 인턴으로 적합성부터 확인")
                .guidingValues("연구 전문성")
                .shakingSituation("희망 기업의 연구개발 직무 제안")
                .shakingSituationDetail("대학원 이외의 경로에서도 연구 역량을 쌓을 수 있다는 가능성을 고려하게 되었어요")
                .clarifiedCriteria("학위보다 체계적인 연구 방법 습득이 중요함")
                .undecidedReason(null)
                .missingInfo("실제 연구실 생활 적합성")
                .disclaimer("이 리포트는 선택의 정답을 평가하지 않습니다.")
                .build();
        setField(Report.class, report, "id", 10L);
        setField(BaseTimeEntity.class, report, "createdAt", LocalDateTime.of(2026, 11, 2, 21, 30, 0));

        ReportAction action1 = ReportAction.builder().reportId(10L).seq(1).content("연구실 인턴 지원하기").build();
        ReportAction action2 = ReportAction.builder().reportId(10L).seq(2).content("선배 면담 요청하기").build();

        given(reportRepository.findBySessionId(sessionId)).willReturn(Optional.of(report));
        given(reportActionRepository.findByReportIdOrderBySeqAsc(10L)).willReturn(List.of(action1, action2));

        ReportResponse response = reportService.getReport(sessionId);

        assertThat(response.reportId()).isEqualTo("10");
        assertThat(response.reportType()).isEqualTo(ReportType.REPORT);
        assertThat(response.entryPoint()).isEqualTo(EntryPoint.S07);
        assertThat(response.concernSummary()).isEqualTo("취업과 대학원 진학 사이의 고민");
        assertThat(response.initialView()).isEqualTo("대학원에 가고 싶다");
        assertThat(response.initialViewReason()).isEqualTo("전문성을 키우고 싶다");
        assertThat(response.revisedView()).isEqualTo("대학원 진학을 유지하되, 연구실 인턴으로 먼저 적합성을 확인한다");
        assertThat(response.revisedViewReason()).isEqualTo("연구실 인턴으로 적합성부터 확인");
        assertThat(response.guidingValues()).isEqualTo("연구 전문성");
        assertThat(response.shakingSituation()).isEqualTo("희망 기업의 연구개발 직무 제안");
        assertThat(response.shakingSituationDetail()).isEqualTo("대학원 이외의 경로에서도 연구 역량을 쌓을 수 있다는 가능성을 고려하게 되었어요");
        assertThat(response.clarifiedCriteria()).isEqualTo("학위보다 체계적인 연구 방법 습득이 중요함");
        assertThat(response.undecidedReason()).isNull();
        assertThat(response.missingInfo()).isEqualTo("실제 연구실 생활 적합성");
        assertThat(response.nextActions()).hasSize(2);
        assertThat(response.nextActions().get(0).seq()).isEqualTo(1);
        assertThat(response.nextActions().get(0).content()).isEqualTo("연구실 인턴 지원하기");
        assertThat(response.disclaimer()).isEqualTo("이 리포트는 선택의 정답을 평가하지 않습니다.");
        assertThat(response.generatedAt()).isNotNull();
    }

    @Test
    void 리포트가_없으면_404_NOT_FOUND_예외를_던진다() {
        given(reportRepository.findBySessionId(sessionId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getReport(sessionId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void reportId로_리포트와_행동_목록을_공개_조회한다() {
        Report report = Report.builder()
                .sessionId(sessionId)
                .reportType(ReportType.REPORT)
                .entryPoint(EntryPoint.S07)
                .concernSummary("취업과 대학원 진학 사이의 고민")
                .initialView("대학원에 가고 싶다")
                .initialViewReason("전문성을 키우고 싶다")
                .revisedView("대학원 진학을 유지하되, 연구실 인턴으로 먼저 적합성을 확인한다")
                .revisedViewReason("연구실 인턴으로 적합성부터 확인")
                .guidingValues("연구 전문성")
                .shakingSituation("희망 기업의 연구개발 직무 제안")
                .shakingSituationDetail("대학원 이외의 경로에서도 연구 역량을 쌓을 수 있다는 가능성을 고려하게 되었어요")
                .clarifiedCriteria("학위보다 체계적인 연구 방법 습득이 중요함")
                .undecidedReason(null)
                .missingInfo("실제 연구실 생활 적합성")
                .disclaimer("이 리포트는 선택의 정답을 평가하지 않습니다.")
                .build();
        setField(Report.class, report, "id", 10L);
        setField(BaseTimeEntity.class, report, "createdAt", LocalDateTime.of(2026, 11, 2, 21, 30, 0));

        ReportAction action1 = ReportAction.builder().reportId(10L).seq(1).content("연구실 인턴 지원하기").build();

        given(reportRepository.findById(10L)).willReturn(Optional.of(report));
        given(reportActionRepository.findByReportIdOrderBySeqAsc(10L)).willReturn(List.of(action1));

        ReportResponse response = reportService.getReportById(10L);

        assertThat(response.reportId()).isEqualTo("10");
        assertThat(response.reportType()).isEqualTo(ReportType.REPORT);
        assertThat(response.concernSummary()).isEqualTo("취업과 대학원 진학 사이의 고민");
        assertThat(response.nextActions()).hasSize(1);
    }

    @Test
    void 공개_조회_시_reportId에_해당하는_리포트가_없으면_404_NOT_FOUND_예외를_던진다() {
        given(reportRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getReportById(999L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    private static void setField(Class<?> clazz, Object target, String fieldName, Object value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
