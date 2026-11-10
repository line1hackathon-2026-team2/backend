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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SessionEndService {

    private final SessionStateService sessionStateService;
    private final ReportGenerator reportGenerator;

    public SessionEndService(SessionStateService sessionStateService, ReportGenerator reportGenerator) {
        this.sessionStateService = sessionStateService;
        this.reportGenerator = reportGenerator;
    }

    /**
     * #13 그만 대화하기 (O2). EXPLORING에서만 가능하다.
     * 리포트 생성(LLM)과 상태 변경을 한 트랜잭션으로 묶어, 생성이 실패하면 상태도 EXPLORING 그대로 남는다.
     */
    @Transactional
    public EndResponse end(UUID sessionId, EndRequest request) {
        if (request.mode() == EndMode.SAVE) {
            // SAVE(→ PAUSED, resumeCode 발급)는 #17 저장/이어하기 작업에서 함께 구현한다.
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "저장 후 종료(SAVE)는 아직 지원하지 않습니다.");
        }

        sessionStateService.requireState(sessionId, SessionState.EXPLORING);

        ReportResult report = reportGenerator.generate(sessionId, ReportType.INCOMPLETE_REPORT, EntryPoint.O2);

        sessionStateService.transition(sessionId, SessionState.REPORT);
        return EndResponse.report(report.reportId());
    }
}
