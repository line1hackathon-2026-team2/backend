package com.decisionlab.session.service;

import com.decisionlab.session.dto.SessionCreateResponse;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.repository.SessionRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SessionService {

    // 같은 사용자의 연속 요청마다 UPDATE가 나가지 않도록, 마지막 연장 후 1분이 지났을 때만 연장한다.
    private static final Duration RENEW_INTERVAL = Duration.ofMinutes(1);

    private final SessionRepository sessionRepository;
    private final SessionTokenGenerator tokenGenerator;
    private final EntityManager entityManager;
    private final Duration sessionTtl;

    public SessionService(SessionRepository sessionRepository,
                          SessionTokenGenerator tokenGenerator,
                          EntityManager entityManager,
                          @Value("${decisionlab.session.ttl:7d}") Duration sessionTtl) {
        this.sessionRepository = sessionRepository;
        this.tokenGenerator = tokenGenerator;
        this.entityManager = entityManager;
        this.sessionTtl = sessionTtl;
    }

    @Transactional
    public SessionCreateResponse createSession() {
        String token = tokenGenerator.generate();
        LocalDateTime expiresAt = LocalDateTime.now().plus(sessionTtl);
        Session session = sessionRepository.save(new Session(token, expiresAt));
        return SessionCreateResponse.from(session);
    }

    /**
     * 만료 시각을 "마지막 접속 + TTL"로 연장한다. 토큰 인증을 통과한 요청마다 필터가 호출한다.
     * 엔티티를 저장하면 state까지 덮어써서 동시에 일어난 상태 전이를 잃을 수 있으므로, expires_at만 갱신한다.
     */
    @Transactional
    public void renewExpiry(UUID sessionId, LocalDateTime currentExpiresAt) {
        LocalDateTime newExpiresAt = LocalDateTime.now().plus(sessionTtl);
        if (currentExpiresAt.plus(RENEW_INTERVAL).isAfter(newExpiresAt)) {
            return;
        }
        entityManager.createQuery("update Session s set s.expiresAt = :expiresAt where s.id = :id")
                .setParameter("expiresAt", newExpiresAt)
                .setParameter("id", sessionId)
                .executeUpdate();
    }
}
