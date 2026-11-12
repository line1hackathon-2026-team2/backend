package com.decisionlab.report.controller;

import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.report.dto.ReportResponse;
import com.decisionlab.report.dto.ReportResponse.NextActionItem;
import com.decisionlab.report.entity.EntryPoint;
import com.decisionlab.report.entity.ReportType;
import com.decisionlab.report.service.ReportService;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.repository.SessionRepository;
import com.decisionlab.session.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReportController.class)
class ReportControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ReportService reportService;

    @MockitoBean
    SessionRepository sessionRepository;

    @MockitoBean
    SessionService sessionService;

    private final UUID sessionId = UUID.randomUUID();
    private final String validToken = "valid-session-token";

    @BeforeEach
    void setUp() throws Exception {
        Session session = new Session(validToken, LocalDateTime.now().plusDays(7));
        Field idField = Session.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(session, sessionId);

        when(sessionRepository.findBySessionToken(validToken)).thenReturn(Optional.of(session));
    }

    @Test
    void 유효한_토큰으로_리포트_조회_시_명세서_4_14_형식으로_응답한다() throws Exception {
        ReportResponse response = sampleReportResponse();
        when(reportService.getReport(sessionId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/report", sessionId)
                        .header("X-Session-Token", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value("1"))
                .andExpect(jsonPath("$.reportType").value("REPORT"))
                .andExpect(jsonPath("$.entryPoint").value("S07"))
                .andExpect(jsonPath("$.concernSummary").value("취업과 대학원 진학 사이의 고민"))
                .andExpect(jsonPath("$.initialView").value("대학원에 가고 싶다"))
                .andExpect(jsonPath("$.initialViewReason").value("전문성을 키우고 싶다"))
                .andExpect(jsonPath("$.revisedView").value("대학원 진학을 유지하되, 연구실 인턴으로 먼저 적합성을 확인한다"))
                .andExpect(jsonPath("$.revisedViewReason").value("연구실 인턴으로 적합성부터 확인"))
                .andExpect(jsonPath("$.guidingValues").value("연구 전문성"))
                .andExpect(jsonPath("$.shakingSituation").value("희망 기업의 연구개발 직무 제안"))
                .andExpect(jsonPath("$.shakingSituationDetail").value("대학원 이외의 경로에서도 연구 역량을 쌓을 수 있다는 가능성을 고려하게 되었어요"))
                .andExpect(jsonPath("$.clarifiedCriteria").value("학위보다 체계적인 연구 방법 습득이 중요함"))
                .andExpect(jsonPath("$.undecidedReason").isEmpty())
                .andExpect(jsonPath("$.missingInfo").value("실제 연구실 생활 적합성"))
                .andExpect(jsonPath("$.nextActions.length()").value(1))
                .andExpect(jsonPath("$.nextActions[0].seq").value(1))
                .andExpect(jsonPath("$.nextActions[0].content").value("연구실 인턴 지원하기"))
                .andExpect(jsonPath("$.disclaimer").value("이 리포트는 선택의 정답을 평가하지 않습니다."))
                .andExpect(jsonPath("$.generatedAt").value("2026-11-02T21:30:00+09:00"));
    }

    @Test
    void 리포트가_없으면_404_응답을_반환한다() throws Exception {
        when(reportService.getReport(sessionId))
                .thenThrow(new ApiException(ErrorCode.NOT_FOUND, "리포트를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/report", sessionId)
                        .header("X-Session-Token", validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void 토큰이_없으면_401_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/{sessionId}/report", sessionId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 다른_세션의_리포트에_접근하면_403_응답을_반환한다() throws Exception {
        UUID otherSessionId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/report", otherSessionId)
                        .header("X-Session-Token", validToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void 세션_토큰_없이_reportId로_리포트_공개_조회_시_200_응답을_반환한다() throws Exception {
        when(reportService.getReportById(1L)).thenReturn(sampleReportResponse());

        mockMvc.perform(get("/api/v1/reports/{reportId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value("1"))
                .andExpect(jsonPath("$.reportType").value("REPORT"))
                .andExpect(jsonPath("$.concernSummary").value("취업과 대학원 진학 사이의 고민"));
    }

    @Test
    void 공개_조회_시_존재하지_않는_reportId면_404_응답을_반환한다() throws Exception {
        when(reportService.getReportById(999L))
                .thenThrow(new ApiException(ErrorCode.NOT_FOUND, "리포트를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/v1/reports/{reportId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void 공개_조회_시_숫자가_아닌_reportId면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/reports/{reportId}", "invalid-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private static ReportResponse sampleReportResponse() {
        return new ReportResponse(
                "1",
                ReportType.REPORT,
                EntryPoint.S07,
                "취업과 대학원 진학 사이의 고민",
                "대학원에 가고 싶다",
                "전문성을 키우고 싶다",
                "대학원 진학을 유지하되, 연구실 인턴으로 먼저 적합성을 확인한다",
                "연구실 인턴으로 적합성부터 확인",
                "연구 전문성",
                "희망 기업의 연구개발 직무 제안",
                "대학원 이외의 경로에서도 연구 역량을 쌓을 수 있다는 가능성을 고려하게 되었어요",
                "학위보다 체계적인 연구 방법 습득이 중요함",
                null,
                "실제 연구실 생활 적합성",
                List.of(new NextActionItem(1, "연구실 인턴 지원하기")),
                "이 리포트는 선택의 정답을 평가하지 않습니다.",
                OffsetDateTime.of(2026, 11, 2, 21, 30, 0, 0, ZoneOffset.ofHours(9))
        );
    }
}
