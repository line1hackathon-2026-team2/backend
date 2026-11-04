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
@Table(name = "dilemma_responses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DilemmaResponse extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dilemma_id", nullable = false)
    private Long dilemmaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "choice", nullable = false, length = 20)
    private DilemmaChoice choice;

    @Column(name = "keep_reason", length = 500)
    private String keepReason;

    @Column(name = "shaken_value", length = 500)
    private String shakenValue;

    @Builder
    public DilemmaResponse(Long dilemmaId, DilemmaChoice choice, String keepReason, String shakenValue) {
        this.dilemmaId = dilemmaId;
        this.choice = choice;
        this.keepReason = keepReason;
        this.shakenValue = shakenValue;
    }
}
