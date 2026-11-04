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

    @Column(name = "initial_preference")
    private String initialPreference;

    @Builder
    public Concern(UUID sessionId, String freeText, ConcernType concernType, String initialPreference) {
        this.sessionId = sessionId;
        this.freeText = freeText;
        this.concernType = concernType;
        this.initialPreference = initialPreference;
    }

    public void update(String freeText, ConcernType concernType, String initialPreference) {
        this.freeText = freeText;
        this.concernType = concernType;
        this.initialPreference = initialPreference;
    }
}
