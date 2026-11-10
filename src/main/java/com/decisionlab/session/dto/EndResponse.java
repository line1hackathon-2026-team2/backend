package com.decisionlab.session.dto;

import com.decisionlab.session.entity.SessionState;

/** #13 응답. INCOMPLETE_REPORT: { "state": "REPORT", "reportId": "..." } */
public record EndResponse(SessionState state, String reportId) {

    public static EndResponse report(Long reportId) {
        return new EndResponse(SessionState.REPORT, String.valueOf(reportId));
    }
}
