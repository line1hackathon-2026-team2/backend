package com.decisionlab.session.dto;

import jakarta.validation.constraints.NotBlank;

/** #17 이어하기 요청. 형식(XXXX-9999) 검증은 대소문자·하이픈을 정리한 뒤 서비스에서 한다. */
public record ResumeRequest(
        @NotBlank(message = "이어하기 코드는 필수입니다.")
        String resumeCode
) {
}
