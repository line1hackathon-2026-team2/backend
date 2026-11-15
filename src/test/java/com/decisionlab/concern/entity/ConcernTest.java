package com.decisionlab.concern.entity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConcernTest {

    private Concern concern(List<String> options, String preference, String reason) {
        return Concern.builder()
                .sessionId(UUID.randomUUID())
                .freeText("취업과 대학원 진학 사이에서 고민 중이에요.")
                .concernType(ConcernType.CAREER)
                .options(options)
                .initialPreference(preference)
                .initialPreferenceReason(reason)
                .build();
    }

    @Test
    void 모든_항목이_있으면_전부_담는다() {
        String text = concern(List.of("취업", "대학원 진학"), "취업", "전문성을 더 키우고 싶어").toPromptText();

        assertThat(text).isEqualTo("""
                고민 유형: 진로(취업·대학원 등)
                고민 내용: 취업과 대학원 진학 사이에서 고민 중이에요.
                비교 중인 선택지: 취업 / 대학원 진학
                지금 마음이 기운 쪽: 취업
                그렇게 느끼는 이유: 전문성을 더 키우고 싶어""");
    }

    @Test
    void 선택지는_있고_고르지_않았으면_아직_모르겠음() {
        String text = concern(List.of("취업", "대학원 진학"), null, null).toPromptText();

        assertThat(text).contains("지금 마음이 기운 쪽: 아직 모르겠음");
        assertThat(text).doesNotContain("그렇게 느끼는 이유");
    }

    @Test
    void 선택지가_없으면_고민_유형과_내용만() {
        String text = concern(null, null, null).toPromptText();

        assertThat(text).isEqualTo("""
                고민 유형: 진로(취업·대학원 등)
                고민 내용: 취업과 대학원 진학 사이에서 고민 중이에요.""");
    }
}
