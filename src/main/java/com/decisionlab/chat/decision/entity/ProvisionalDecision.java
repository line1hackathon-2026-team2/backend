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

    @Column(name = "core_value", length = 200)
    private String coreValue;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "acceptable_cost", length = 500)
    private String acceptableCost;

    @Column(name = "unchecked_condition", length = 500)
    private String uncheckedCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolved_action", length = 20)
    private DecisionAction resolvedAction;

    @Builder
    public ProvisionalDecision(UUID sessionId, Integer revisionNo, String summaryText, String decisionText,
                                String coreValue, String reason, String acceptableCost, String uncheckedCondition) {
        this.sessionId = sessionId;
        this.revisionNo = revisionNo;
        this.summaryText = summaryText;
        this.decisionText = decisionText;
        this.coreValue = coreValue;
        this.reason = reason;
        this.acceptableCost = acceptableCost;
        this.uncheckedCondition = uncheckedCondition;
    }

    public void resolve(DecisionAction action) {
        this.resolvedAction = action;
    }
}
