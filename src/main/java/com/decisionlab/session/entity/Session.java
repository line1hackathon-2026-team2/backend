package com.decisionlab.session.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Session extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, updatable = false, nullable = false)
    private UUID id;

    @Column(name = "session_token", nullable = false, unique = true, length = 512)
    private String sessionToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 30)
    private SessionState state;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public Session(String sessionToken, LocalDateTime expiresAt) {
        this.sessionToken = sessionToken;
        this.state = SessionState.CREATED;
        this.expiresAt = expiresAt;
    }

    public void transitionTo(SessionState next) {
        this.state = next;
    }

    public void renewExpiry(LocalDateTime newExpiresAt) {
        this.expiresAt = newExpiresAt;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
}
