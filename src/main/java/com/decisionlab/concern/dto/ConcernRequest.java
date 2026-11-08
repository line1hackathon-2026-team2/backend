package com.decisionlab.concern.dto;

import com.decisionlab.concern.entity.ConcernType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 명세서 4.3 고민 입력 요청.
 * options 개수(0 또는 2~3개)·중복, initialPreference가 options 안에 있는지는 ConcernService에서 확인한다.
 */
public record ConcernRequest(
        @NotBlank(message = "고민 내용은 필수입니다.")
        @Size(max = 1000, message = "고민 내용은 1~1000자로 입력해 주세요.")
        String freeText,

        @NotNull(message = "고민 유형은 필수입니다.")
        ConcernType concernType,

        List<@NotBlank(message = "선택지는 빈 값일 수 없습니다.")
             @Size(max = 15, message = "선택지는 15자 이하로 입력해 주세요.") String> options,

        @Size(max = 15, message = "초기 선호는 15자 이하로 입력해 주세요.")
        String initialPreference,

        @Size(max = 100, message = "선택한 이유는 100자 이하로 입력해 주세요.")
        String initialPreferenceReason
) {
}
