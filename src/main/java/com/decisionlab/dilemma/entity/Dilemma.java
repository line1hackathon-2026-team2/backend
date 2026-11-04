package com.decisionlab.dilemma.entity;

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
@Table(name = "dilemmas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dilemma extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "session_id", nullable = false, length = 36)
    private UUID sessionId;

    @Column(name = "decision_id", nullable = false)
    private Long decisionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dilemma_type", nullable = false, length = 30)
    private DilemmaType dilemmaType;

    @Column(name = "scenario_text", nullable = false, length = 1000)
    private String scenarioText;

    @Column(name = "verification_goal", nullable = false, length = 300)
    private String verificationGoal;

    @Column(name = "is_fallback", nullable = false)
    private boolean fallback;

    @Builder
    public Dilemma(UUID sessionId, Long decisionId, DilemmaType dilemmaType, String scenarioText,
                    String verificationGoal, boolean fallback) {
        this.sessionId = sessionId;
        this.decisionId = decisionId;
        this.dilemmaType = dilemmaType;
        this.scenarioText = scenarioText;
        this.verificationGoal = verificationGoal;
        this.fallback = fallback;
    }
}
