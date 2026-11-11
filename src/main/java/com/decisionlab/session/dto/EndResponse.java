package com.decisionlab.session.dto;

import com.decisionlab.session.entity.SessionState;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * #13 응답 (명세서 4.13). 모드마다 한쪽 값만 있으므로 null인 키는 내려주지 않는다.
 * - INCOMPLETE_REPORT: { "state": "REPORT", "reportId": "..." }
 * - SAVE:              { "state": "PAUSED", "resumeCode": "ABCD-1234" }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EndResponse(SessionState state, String reportId, String resumeCode) {

    public static EndResponse report(Long reportId) {
        return new EndResponse(SessionState.REPORT, String.valueOf(reportId), null);
    }

    public static EndResponse paused(String resumeCode) {
        return new EndResponse(SessionState.PAUSED, null, resumeCode);
    }
}
