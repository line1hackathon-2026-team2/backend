package com.decisionlab.ai.report;

import com.decisionlab.ai.llm.LlmClient;
import com.decisionlab.ai.report.LlmReportGenerator.GeneratedReport;
import com.decisionlab.chat.criterion.repository.JudgmentCriterionRepository;
import com.decisionlab.chat.decision.repository.ProvisionalDecisionRepository;
import com.decisionlab.chat.message.repository.MessageRepository;
import com.decisionlab.concern.repository.ConcernRepository;
import com.decisionlab.dilemma.repository.DilemmaRepository;
import com.decisionlab.dilemma.repository.DilemmaResponseRepository;
import com.decisionlab.global.entity.BaseTimeEntity;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.persona.repository.PersonaRepository;
import com.decisionlab.report.dto.ReportResult;
import com.decisionlab.report.entity.EntryPoint;
import com.decisionlab.report.entity.Report;
import com.decisionlab.report.entity.ReportType;
import com.decisionlab.report.repository.ReportActionRepository;
import com.decisionlab.report.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LlmReportGeneratorTest {

    @Mock
    ReportRepository reportRepository;

    @Mock
    ReportActionRepository reportActionRepository;

    @Mock
    DilemmaRepository dilemmaRepository;

    @Mock
    DilemmaResponseRepository dilemmaResponseRepository;

    @Mock
    ConcernRepository concernRepository;

    @Mock
    MessageRepository messageRepository;

    @Mock
    PersonaRepository personaRepository;

    @Mock
    ProvisionalDecisionRepository provisionalDecisionRepository;

    @Mock
    JudgmentCriterionRepository judgmentCriterionRepository;

    @Mock
    LlmClient llmClient;

    @InjectMocks
    LlmReportGenerator llmReportGenerator;

    private final UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        given(concernRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
        given(messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId)).willReturn(List.of());
        given(provisionalDecisionRepository.findBySessionIdOrderByRevisionNoDesc(sessionId)).willReturn(List.of());
        given(dilemmaRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
    }

    @Test
    void REPORT_유형_생성_시_모든_필드와_nextActions를_저장하고_undecidedReason은_null로_반환한다() {
        GeneratedReport generated = new GeneratedReport(
                "취업과 대학원 진학 사이의 고민",
                "대학원에 가고 싶다",
                "전문성을 키우고 싶다",
                "대학원 진학을 유지하되, 연구실 인턴으로 먼저 적합성을 확인한다",
                "연구실 인턴으로 적합성부터 확인",
                "연구 전문성",
                "희망 기업의 연구개발 직무 제안",
                "대학원 이외의 경로에서도 연구 역량을 쌓을 수 있다는 가능성을 고려하게 되었어요",
                "학위보다 체계적인 연구 방법 습득이 중요함",
                "실수로 들어온 미결정 이유",
                "실제 연구실 생활 적합성",
                List.of("연구실 인턴 지원하기", "선배 면담 요청하기")
        );
        given(llmClient.call(eq("report-generator"), any(), eq(GeneratedReport.class))).willReturn(generated);
        given(reportRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
        given(reportRepository.save(any(Report.class))).willAnswer(invocation -> {
            Report r = invocation.getArgument(0);
            setField(Report.class, r, "id", 10L);
            return r;
        });
        given(reportActionRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        ReportResult result = llmReportGenerator.generate(sessionId, ReportType.REPORT, EntryPoint.S07);

        assertThat(result.reportId()).isEqualTo(10L);
        assertThat(result.reportType()).isEqualTo(ReportType.REPORT);
        assertThat(result.entryPoint()).isEqualTo(EntryPoint.S07);
        assertThat(result.initialView()).isEqualTo("대학원에 가고 싶다");
        assertThat(result.initialViewReason()).isEqualTo("전문성을 키우고 싶다");
        assertThat(result.revisedView()).isEqualTo("대학원 진학을 유지하되, 연구실 인턴으로 먼저 적합성을 확인한다");
        assertThat(result.revisedViewReason()).isEqualTo("연구실 인턴으로 적합성부터 확인");
        assertThat(result.shakingSituation()).isEqualTo("희망 기업의 연구개발 직무 제안");
        assertThat(result.shakingSituationDetail())
                .isEqualTo("대학원 이외의 경로에서도 연구 역량을 쌓을 수 있다는 가능성을 고려하게 되었어요");
        assertThat(result.undecidedReason()).isNull();
        assertThat(result.nextActions()).hasSize(2);
        assertThat(result.nextActions().get(0).seq()).isEqualTo(1);
        assertThat(result.nextActions().get(0).content()).isEqualTo("연구실 인턴 지원하기");
    }

    @Test
    void UNDECIDED_REPORT_S05_생성_시_initialView_revisedView_shakingSituation은_null로_강제된다() {
        GeneratedReport generated = new GeneratedReport(
                "비교 중인 선택지",
                "잘못 들어온 초기 관점",
                "잘못 들어온 초기 이유",
                "잘못 들어온 최종 관점",
                "잘못 들어온 최종 이유",
                "연구 전문성과 경제적 안정성",
                "잘못 들어온 딜레마 상황",
                "잘못 들어온 딜레마 설명",
                "잘못 들어온 기준",
                "충분히 비교하지 못함",
                "현업 R&D 직무 범위",
                List.of("JD 비교하기")
        );
        given(llmClient.call(eq("report-generator"), any(), eq(GeneratedReport.class))).willReturn(generated);
        given(reportRepository.findBySessionId(sessionId)).willReturn(Optional.empty());
        given(reportRepository.save(any(Report.class))).willAnswer(invocation -> {
            Report r = invocation.getArgument(0);
            setField(Report.class, r, "id", 11L);
            return r;
        });
        given(reportActionRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        ReportResult result = llmReportGenerator.generate(sessionId, ReportType.UNDECIDED_REPORT, EntryPoint.S05);

        assertThat(result.reportId()).isEqualTo(11L);
        assertThat(result.initialView()).isNull();
        assertThat(result.initialViewReason()).isNull();
        assertThat(result.revisedView()).isNull();
        assertThat(result.revisedViewReason()).isNull();
        assertThat(result.shakingSituation()).isNull();
        assertThat(result.shakingSituationDetail()).isNull();
        assertThat(result.clarifiedCriteria()).isNull();
        assertThat(result.undecidedReason()).isEqualTo("충분히 비교하지 못함");
    }

    @Test
    void 기존_리포트가_존재하면_같은_reportId로_덮어쓰고_report_actions를_교체한다() {
        Report existing = Report.builder()
                .sessionId(sessionId)
                .reportType(ReportType.INCOMPLETE_REPORT)
                .entryPoint(EntryPoint.O2)
                .concernSummary("기존 미완료 요약")
                .guidingValues("기존 가치")
                .missingInfo("기존 부족 정보")
                .disclaimer("고지")
                .build();
        setField(Report.class, existing, "id", 15L);
        LocalDateTime updatedTime = LocalDateTime.of(2026, 11, 11, 15, 0, 0);
        setField(BaseTimeEntity.class, existing, "updatedAt", updatedTime);

        GeneratedReport generated = new GeneratedReport(
                "재개 후 완료된 고민 요약",
                "초기 생각",
                "초기 이유",
                "최종 생각",
                "최종 이유",
                "핵심 가치",
                "딜레마 상황",
                "딜레마 상세",
                "명확해진 기준",
                null,
                "추가 확인 정보",
                List.of("새 행동 1")
        );
        given(llmClient.call(eq("report-generator"), any(), eq(GeneratedReport.class))).willReturn(generated);
        given(reportRepository.findBySessionId(sessionId)).willReturn(Optional.of(existing));
        given(reportRepository.saveAndFlush(existing)).willReturn(existing);
        given(reportActionRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        ReportResult result = llmReportGenerator.generate(sessionId, ReportType.REPORT, EntryPoint.S07);

        verify(reportActionRepository).deleteByReportId(15L);
        assertThat(result.reportId()).isEqualTo(15L);
        assertThat(result.reportType()).isEqualTo(ReportType.REPORT);
        assertThat(result.concernSummary()).isEqualTo("재개 후 완료된 고민 요약");
        assertThat(result.generatedAt()).isEqualTo(updatedTime);
    }

    @Test
    void LLM_호출_실패_시_LLM_FAILED_예외가_그대로_전파된다() {
        given(llmClient.call(eq("report-generator"), any(), eq(GeneratedReport.class)))
                .willThrow(new ApiException(ErrorCode.LLM_FAILED, "LLM 호출 실패"));

        assertThatThrownBy(() -> llmReportGenerator.generate(sessionId, ReportType.REPORT, EntryPoint.S07))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.LLM_FAILED);
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
