package com.decisionlab.session.entity;

// #13 그만 대화하기 방식 (4.13)
public enum EndMode {
    /** 미완료 리포트를 만들고 종료 (EXPLORING → REPORT) */
    INCOMPLETE_REPORT,
    /** 저장 후 종료 (EXPLORING → PAUSED + 이어하기 코드 발급). #17 이어하기와 짝 */
    SAVE
}
