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

@Entity
@Table(name = "dilemma_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DilemmaTemplate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "dilemma_type", nullable = false, length = 30)
    private DilemmaType dilemmaType;

    @Column(name = "scenario_text", nullable = false, length = 1000)
    private String scenarioText;

    @Column(name = "verification_goal", nullable = false, length = 300)
    private String verificationGoal;

    @Builder
    public DilemmaTemplate(DilemmaType dilemmaType, String scenarioText, String verificationGoal) {
        this.dilemmaType = dilemmaType;
        this.scenarioText = scenarioText;
        this.verificationGoal = verificationGoal;
    }
}
