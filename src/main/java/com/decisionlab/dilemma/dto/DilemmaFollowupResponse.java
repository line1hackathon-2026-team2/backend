package com.decisionlab.dilemma.dto;

import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.session.entity.SessionState;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "딜레마 후속 입력 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DilemmaFollowupResponse(
        @Schema(description = "전환된 세션 상태", example = "REPORT")
        SessionState state,

        @Schema(description = "리포트 ID (REFLECTION -> REPORT 전환 시 반환)", example = "15", nullable = true)
        String reportId,

        @Schema(description = "재검토 횟수 (RECONSIDER -> EXPLORING 전환 시 반환)", example = "1", nullable = true)
        Integer reconsiderCount,

        @Schema(description = "추천 전환 자아 코드 (RECONSIDER -> EXPLORING 전환 시 반환)", example = "STABILITY", nullable = true)
        PersonaCode suggestedPersonaCode
) {
    public static DilemmaFollowupResponse keep(Long reportId) {
        return new DilemmaFollowupResponse(SessionState.REPORT, String.valueOf(reportId), null, null);
    }

    public static DilemmaFollowupResponse reconsider(int reconsiderCount, PersonaCode suggestedPersonaCode) {
        return new DilemmaFollowupResponse(SessionState.EXPLORING, null, reconsiderCount, suggestedPersonaCode);
    }
}
