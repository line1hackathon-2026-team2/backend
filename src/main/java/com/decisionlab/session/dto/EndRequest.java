package com.decisionlab.session.dto;

import jakarta.validation.constraints.NotNull;

public record EndRequest(
        @NotNull(message = "종료 방식(mode)은 필수입니다.")
        EndMode mode
) {
}
