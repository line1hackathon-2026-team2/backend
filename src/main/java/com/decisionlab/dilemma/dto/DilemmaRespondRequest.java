package com.decisionlab.dilemma.dto;

import com.decisionlab.dilemma.entity.DilemmaChoice;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 명세서 4.10 딜레마 응답 요청 */
public record DilemmaRespondRequest(
        @Schema(description = "딜레마에 대한 선택 (KEEP / RECONSIDER / UNDECIDED)", example = "KEEP")
        @NotNull(message = "딜레마 선택 값은 필수입니다.")
        DilemmaChoice choice
) {
}
