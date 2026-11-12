package com.decisionlab.dilemma.dto;

import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.session.entity.SessionState;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 명세서 4.11 딜레마 응답 결과 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DilemmaRespondResponse(
        @Schema(description = "전이된 세션 상태 (REFLECTION / RECONSIDER / REPORT)", example = "REFLECTION")
        SessionState state,

        @Schema(description = "후속 질문 프롬프트 (KEEP, RECONSIDER 선택 시)",
                example = "새로운 조건이 생겼는데도 같은 선택을 유지하는 이유가 무엇인가요?")
        String prompt,

        @Schema(description = "후속 질문 선택지 목록 (KEEP, RECONSIDER 선택 시)")
        List<MessageOption> options,

        @Schema(description = "추천 전환 자아 코드 (RECONSIDER 선택 시)", example = "STABILITY")
        PersonaCode suggestedPersonaCode,

        @Schema(description = "생성된 미결정 리포트 ID (UNDECIDED 선택 시, 숫자 문자열)", example = "1")
        String reportId
) {

    public static DilemmaRespondResponse keep(String prompt, List<MessageOption> options) {
        return new DilemmaRespondResponse(SessionState.REFLECTION, prompt, options, null, null);
    }

    public static DilemmaRespondResponse reconsider(String prompt, List<MessageOption> options,
                                                    PersonaCode suggestedPersonaCode) {
        return new DilemmaRespondResponse(SessionState.RECONSIDER, prompt, options, suggestedPersonaCode, null);
    }

    public static DilemmaRespondResponse undecided(Long reportId) {
        return new DilemmaRespondResponse(
                SessionState.REPORT,
                null,
                null,
                null,
                reportId != null ? String.valueOf(reportId) : null
        );
    }
}
