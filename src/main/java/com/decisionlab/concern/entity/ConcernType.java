package com.decisionlab.concern.entity;

public enum ConcernType {
    CAREER("진로(취업·대학원 등)"),
    MAJOR("전공·학업"),
    LEAVE("휴학·시기"),
    OTHER("기타");

    private final String label;

    ConcernType(String label) {
        this.label = label;
    }

    /** AI 프롬프트용 한글 이름 (명세서 4.3) */
    public String getLabel() {
        return label;
    }
}
