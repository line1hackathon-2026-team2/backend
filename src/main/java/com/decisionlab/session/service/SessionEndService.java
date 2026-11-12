package com.decisionlab.session.service;

import com.decisionlab.ai.report.ReportGenerator;
import com.decisionlab.report.dto.ReportResult;
import com.decisionlab.report.entity.EntryPoint;
import com.decisionlab.report.entity.ReportType;
import com.decisionlab.session.entity.EndMode;
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
    private final SessionSaveService sessionSaveService;

    public SessionEndService(SessionStateService sessionStateService, ReportGenerator reportGenerator,
                             SessionSaveService sessionSaveService) {
        this.sessionStateService = sessionStateService;
        this.reportGenerator = reportGenerator;
        this.sessionSaveService = sessionSaveService;
    }

    /**
     * #13 그만 대화하기 (O2). 두 모드 모두 EXPLORING에서만 가능하다.
     * - INCOMPLETE_REPORT: 미완료 리포트 생성 후 REPORT. 리포트 생성(LLM)이 실패하면 상태도 EXPLORING 그대로 남는다.
     * - SAVE: PAUSED로 바꾸고 이어하기 코드 발급 (#17과 짝)
     */
    @Transactional
    public EndResponse end(UUID sessionId, EndRequest request) {
        if (request.mode() == EndMode.SAVE) {
            return EndResponse.paused(sessionSaveService.save(sessionId));
        }

        sessionStateService.requireState(sessionId, SessionState.EXPLORING);

        ReportResult report = reportGenerator.generate(sessionId, ReportType.INCOMPLETE_REPORT, EntryPoint.O2);

        sessionStateService.transition(sessionId, SessionState.REPORT);
        return EndResponse.report(report.reportId());
    }
}
