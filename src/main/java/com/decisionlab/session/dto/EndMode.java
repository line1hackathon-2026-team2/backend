package com.decisionlab.session.dto;

// #13 그만 대화하기 방식 (4.13)
public enum EndMode {
    /** 미완료 리포트를 만들고 종료 (EXPLORING → REPORT) */
    INCOMPLETE_REPORT,
    /** 저장 후 종료 (EXPLORING → PAUSED). P1, #17 저장/이어하기 작업에서 구현 예정 */
    SAVE
}
