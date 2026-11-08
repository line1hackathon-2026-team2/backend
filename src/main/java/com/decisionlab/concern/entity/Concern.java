package com.decisionlab.concern.entity;

import com.decisionlab.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "concerns")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Concern extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "session_id", nullable = false, length = 36)
    private UUID sessionId;

    @Column(name = "free_text", nullable = false, length = 1000)
    private String freeText;

    @Enumerated(EnumType.STRING)
    @Column(name = "concern_type", nullable = false, length = 20)
    private ConcernType concernType;

    /** 비교 중인 선택지 (입력 순서 유지). 없으면 null, 있으면 2~3개 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options")
    private List<String> options;

    /** options 중 사용자가 고른 하나. "아직 모르겠어요"면 null */
    @Column(name = "initial_preference")
    private String initialPreference;

    @Column(name = "initial_preference_reason", length = 100)
    private String initialPreferenceReason;

    @Builder
    public Concern(UUID sessionId, String freeText, ConcernType concernType, List<String> options,
                   String initialPreference, String initialPreferenceReason) {
        this.sessionId = sessionId;
        this.freeText = freeText;
        this.concernType = concernType;
        this.options = options;
        this.initialPreference = initialPreference;
        this.initialPreferenceReason = initialPreferenceReason;
    }

    public void update(String freeText, ConcernType concernType, List<String> options,
                       String initialPreference, String initialPreferenceReason) {
        this.freeText = freeText;
        this.concernType = concernType;
        this.options = options;
        this.initialPreference = initialPreference;
        this.initialPreferenceReason = initialPreferenceReason;
    }

    /**
     * AI 대화(Conversation Engine)와 리포트(concernSummary "오늘의 고민")에 넘길 고민 요약 텍스트.
     * 저장은 필드별로 하고, AI에는 이 텍스트 하나로 넘긴다. 입력하지 않은 항목은 줄을 생략한다.
     */
    public String toPromptText() {
        StringBuilder text = new StringBuilder()
                .append("고민 유형: ").append(concernType.getLabel()).append('\n')
                .append("고민 내용: ").append(freeText);
        if (options != null && !options.isEmpty()) {
            text.append('\n').append("비교 중인 선택지: ").append(String.join(" / ", options));
            text.append('\n').append("지금 마음이 기운 쪽: ")
                    .append(initialPreference != null ? initialPreference : "아직 모르겠음");
        }
        if (initialPreferenceReason != null) {
            text.append('\n').append("그렇게 느끼는 이유: ").append(initialPreferenceReason);
        }
        return text.toString();
    }
}
