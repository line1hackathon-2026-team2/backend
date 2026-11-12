package com.decisionlab.session.service;

import com.decisionlab.session.dto.SessionCreateResponse;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.repository.SessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTest {

    private final SessionRepository sessionRepository = mock(SessionRepository.class);
    private final SessionTokenGenerator tokenGenerator = mock(SessionTokenGenerator.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final SessionService sessionService =
            new SessionService(sessionRepository, tokenGenerator, entityManager, Duration.ofDays(7));

    @Test
    void 세션을_CREATED_상태와_7일_만료로_생성한다() {
        when(tokenGenerator.generate()).thenReturn("test-token");
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionCreateResponse response = sessionService.createSession();

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        Session saved = captor.getValue();

        assertThat(saved.getSessionToken()).isEqualTo("test-token");
        assertThat(saved.getState()).isEqualTo(SessionState.CREATED);
        assertThat(saved.getExpiresAt()).isCloseTo(LocalDateTime.now().plusDays(7), within(5, SECONDS));

        assertThat(response.sessionToken()).isEqualTo("test-token");
        assertThat(response.state()).isEqualTo(SessionState.CREATED);
        assertThat(response.expiresAt().getOffset().getTotalSeconds()).isEqualTo(9 * 3600);
        assertThat(response.expiresAt().toLocalDateTime())
                .isCloseTo(saved.getExpiresAt().atZone(java.time.ZoneId.systemDefault())
                        .withZoneSameInstant(java.time.ZoneId.of("Asia/Seoul")).toLocalDateTime(), within(1, SECONDS));
    }

    @Test
    void 마지막_연장_후_1분이_지났으면_만료를_지금부터_7일로_연장한다() {
        Query query = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        UUID id = UUID.randomUUID();

        sessionService.renewExpiry(id, LocalDateTime.now().plusDays(3));

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(query).setParameter(eq("expiresAt"), captor.capture());
        verify(query).setParameter("id", id);
        verify(query).executeUpdate();
        assertThat(captor.getValue()).isCloseTo(LocalDateTime.now().plusDays(7), within(5, SECONDS));
    }

    @Test
    void 방금_연장했으면_다시_UPDATE하지_않는다() {
        sessionService.renewExpiry(UUID.randomUUID(), LocalDateTime.now().plusDays(7).minusSeconds(10));

        verify(entityManager, never()).createQuery(anyString());
    }
}
