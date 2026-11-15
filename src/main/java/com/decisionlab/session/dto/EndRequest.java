package com.decisionlab.session.dto;

import com.decisionlab.session.entity.EndMode;
import jakarta.validation.constraints.NotNull;

public record EndRequest(
        @NotNull(message = "종료 방식은 필수입니다.")
        EndMode mode
) {
}
