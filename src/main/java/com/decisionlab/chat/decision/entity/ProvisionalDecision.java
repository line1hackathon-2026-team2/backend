package com.decisionlab.chat.decision.entity;

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

import java.util.UUID;

@Entity
@Table(name = "provisional_decisions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProvisionalDecision extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "session_id", nullable = false, length = 36)
    private UUID sessionId;

    @Column(name = "revision_no", nullable = false)
    private Integer revisionNo;

    @Column(name = "summary_text", nullable = false, length = 1000)
    private String summaryText;

    @Column(name = "decision_text", length = 200)
    private String decisionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_text_source", length = 20)
    private FieldSource decisionTextSource;

    @Column(name = "core_value", length = 200)
    private String coreValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "core_value_source", length = 20)
    private FieldSource coreValueSource;

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_source", length = 20)
    private FieldSource reasonSource;

    @Column(name = "acceptable_cost", length = 500)
    private String acceptableCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "acceptable_cost_source", length = 20)
    private FieldSource acceptableCostSource;

    @Column(name = "unchecked_condition", length = 500)
    private String uncheckedCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolved_action", length = 20)
    private DecisionAction resolvedAction;

    @Builder
    public ProvisionalDecision(UUID sessionId, Integer revisionNo, String summaryText, String decisionText,
                                FieldSource decisionTextSource, String coreValue, FieldSource coreValueSource,
                                String reason, FieldSource reasonSource, String acceptableCost,
                                FieldSource acceptableCostSource, String uncheckedCondition) {
        this.sessionId = sessionId;
        this.revisionNo = revisionNo;
        this.summaryText = summaryText;
        this.decisionText = decisionText;
        this.decisionTextSource = decisionTextSource;
        this.coreValue = coreValue;
        this.coreValueSource = coreValueSource;
        this.reason = reason;
        this.reasonSource = reasonSource;
        this.acceptableCost = acceptableCost;
        this.acceptableCostSource = acceptableCostSource;
        this.uncheckedCondition = uncheckedCondition;
    }

    public void resolve(DecisionAction action) {
        this.resolvedAction = action;
    }

    /**
     * #9 필드 단위 수정(PATCH). revisionNo는 올리지 않고 같은 리비전의 값만 덮어쓴다.
     * field는 서비스에서 이미 검증된 값(decisionText/coreValue/reason/acceptableCost/uncheckedCondition)만 들어온다.
     * 사용자가 직접 고친 필드의 source는 USER_STATED로 갱신한다. uncheckedCondition은 "검증 여부" 플래그라
     * 별도 source 컬럼이 없고 항상 NEEDS_CHECK이므로 대상에서 제외한다.
     */
    public void updateField(String field, String value) {
        switch (field) {
            case "decisionText" -> {
                this.decisionText = value;
                this.decisionTextSource = FieldSource.USER_STATED;
            }
            case "coreValue" -> {
                this.coreValue = value;
                this.coreValueSource = FieldSource.USER_STATED;
            }
            case "reason" -> {
                this.reason = value;
                this.reasonSource = FieldSource.USER_STATED;
            }
            case "acceptableCost" -> {
                this.acceptableCost = value;
                this.acceptableCostSource = FieldSource.USER_STATED;
            }
            case "uncheckedCondition" -> this.uncheckedCondition = value;
            default -> throw new IllegalArgumentException("알 수 없는 필드: " + field);
        }
    }
}
