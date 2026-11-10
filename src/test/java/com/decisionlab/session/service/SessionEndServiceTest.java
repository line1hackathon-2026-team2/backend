package com.decisionlab.session.service;

import com.decisionlab.ai.report.ReportGenerator;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.report.dto.ReportResult;
import com.decisionlab.report.entity.EntryPoint;
import com.decisionlab.report.entity.ReportType;
import com.decisionlab.session.dto.EndMode;
import com.decisionlab.session.dto.EndRequest;
import com.decisionlab.session.dto.EndResponse;
import com.decisionlab.session.entity.SessionState;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SessionEndServiceTest {

    private final SessionStateService sessionStateService = mock(SessionStateService.class);
    private final ReportGenerator reportGenerator = mock(ReportGenerator.class);
    private final SessionEndService sessionEndService = new SessionEndService(sessionStateService, reportGenerator);

    private final UUID sessionId = UUID.randomUUID();

    private ReportResult incompleteReport(Long reportId) {
        return new ReportResult(reportId, ReportType.INCOMPLETE_REPORT, EntryPoint.O2,
                "취업과 대학원 사이 고민", null, null, "연구 전문성", null, null, null,
                "실제 연구실 생활 적합성", List.of(), "이 리포트는 선택의 정답을 평가하지 않습니다.", LocalDateTime.now());
    }

    @Test
    void 미완료_리포트를_만들고_REPORT로_바꾼_뒤_reportId를_문자열로_돌려준다() {
        when(reportGenerator.generate(sessionId, ReportType.INCOMPLETE_REPORT, EntryPoint.O2))
                .thenReturn(incompleteReport(15L));

        EndResponse response = sessionEndService.end(sessionId, new EndRequest(EndMode.INCOMPLETE_REPORT));

        assertThat(response.state()).isEqualTo(SessionState.REPORT);
        assertThat(response.reportId()).isEqualTo("15");

        // 상태 검증 → 리포트 생성 → 상태 전이 순서
        InOrder order = inOrder(sessionStateService, reportGenerator);
        order.verify(sessionStateService).requireState(sessionId, SessionState.EXPLORING);
        order.verify(reportGenerator).generate(sessionId, ReportType.INCOMPLETE_REPORT, EntryPoint.O2);
        order.verify(sessionStateService).transition(sessionId, SessionState.REPORT);
    }

    @Test
    void EXPLORING이_아니면_409이고_리포트를_만들지_않는다() {
        doThrow(new ApiException(ErrorCode.INVALID_STATE_TRANSITION, "불가", "DILEMMA"))
                .when(sessionStateService).requireState(sessionId, SessionState.EXPLORING);

        assertThatThrownBy(() -> sessionEndService.end(sessionId, new EndRequest(EndMode.INCOMPLETE_REPORT)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);

        verifyNoInteractions(reportGenerator);
        verify(sessionStateService, never()).transition(any(), any());
    }

    @Test
    void 리포트_생성이_실패하면_상태를_바꾸지_않는다() {
        when(reportGenerator.generate(sessionId, ReportType.INCOMPLETE_REPORT, EntryPoint.O2))
                .thenThrow(new ApiException(ErrorCode.LLM_FAILED, "리포트 생성 실패"));

        assertThatThrownBy(() -> sessionEndService.end(sessionId, new EndRequest(EndMode.INCOMPLETE_REPORT)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LLM_FAILED);

        verify(sessionStateService, never()).transition(any(), any());
    }

    @Test
    void SAVE는_아직_지원하지_않아_400이고_아무것도_하지_않는다() {
        assertThatThrownBy(() -> sessionEndService.end(sessionId, new EndRequest(EndMode.SAVE)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);

        verifyNoInteractions(sessionStateService, reportGenerator);
    }
}
