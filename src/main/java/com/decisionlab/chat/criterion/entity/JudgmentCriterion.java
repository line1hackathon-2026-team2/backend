package com.decisionlab.chat.criterion.entity;

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
@Table(name = "judgment_criteria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JudgmentCriterion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "session_id", nullable = false, length = 36)
    private UUID sessionId;

    @Column(name = "decision_id", nullable = false)
    private Long decisionId;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private CriterionSource source;

    @Builder
    public JudgmentCriterion(UUID sessionId, Long decisionId, String category, String content, CriterionSource source) {
        this.sessionId = sessionId;
        this.decisionId = decisionId;
        this.category = category;
        this.content = content;
        this.source = source;
    }
}
