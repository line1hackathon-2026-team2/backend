package com.decisionlab.chat.decision.dto;

import com.decisionlab.dilemma.dto.DilemmaResult;
import com.decisionlab.dilemma.entity.DilemmaType;
import com.decisionlab.session.entity.SessionState;

/** 명세서 4.10 잠정 결정 처리 응답 */
public record ResolveResponse(
        SessionState state,
        DilemmaItem dilemma,
        String reportId
) {

    public record DilemmaItem(
            String dilemmaId,
            DilemmaType dilemmaType,
            String title,
            String scenarioText,
            String verificationGoal,
            boolean isFallback
    ) {
        // TODO(C): DilemmaResult/Dilemma에 title 필드가 아직 없어 null로 내려간다.
        // 명세서 4.10 "title은 Dilemma Generator가 scenarioText와 함께 생성" 참고 — 추가되면 교체.
        public static DilemmaItem from(DilemmaResult result) {
            return new DilemmaItem(
                    String.valueOf(result.dilemmaId()),
                    result.dilemmaType(),
                    null,
                    result.scenarioText(),
                    result.verificationGoal(),
                    result.isFallback()
            );
        }
    }

    public static ResolveResponse toDilemma(DilemmaResult result) {
        return new ResolveResponse(SessionState.DILEMMA, DilemmaItem.from(result), null);
    }

    public static ResolveResponse toReport(Long reportId) {
        return new ResolveResponse(SessionState.REPORT, null, String.valueOf(reportId));
    }

    public static ResolveResponse toExploring() {
        return new ResolveResponse(SessionState.EXPLORING, null, null);
    }
}
