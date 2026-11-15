package com.decisionlab.dilemma.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "딜레마 후속 입력 요청 (유지 이유 / 흔들린 가치 입력)")
public record DilemmaFollowupRequest(
        @Schema(description = "유지 이유 (S07 REFLECTION 상태에서 입력)", example = "체계적으로 연구 방법을 배우는 과정이 더 중요하다고 생각해요.", nullable = true)
        @Size(max = 500, message = "keepReason은 500자 이하여야 합니다.")
        String keepReason,

        @Schema(description = "흔들린 가치 (S08 RECONSIDER 상태에서 입력)", example = "좋은 기업에서 일할 기회를 포기하는 게 아까워졌어요.", nullable = true)
        @Size(max = 500, message = "shakenValue는 500자 이하여야 합니다.")
        String shakenValue
) {
}
