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

    /** #17 이어하기 코드 (XXXX-9999). 저장(SAVE)할 때마다 새 코드로 교체되고, 이어하기 후에도 다음 SAVE 전까지 남아 있다. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "resume_code", unique = true, length = 9)
    private String resumeCode;

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

    /** SAVE 시 새 이어하기 코드로 교체한다. 이전 코드는 이때 더 이상 쓸 수 없게 된다. */
    public void assignResumeCode(String resumeCode) {
        this.resumeCode = resumeCode;
    }

    /** 이어하기 시 새 토큰으로 교체한다. 토큰은 세션당 하나라 이전 기기의 토큰은 자동으로 끊긴다. */
    public void rotateToken(String newToken, LocalDateTime newExpiresAt) {
        this.sessionToken = newToken;
        this.expiresAt = newExpiresAt;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
}
