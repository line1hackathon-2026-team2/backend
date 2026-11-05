package com.decisionlab.session.service;

import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SessionStateServiceImplTest {

    @Mock
    SessionRepository sessionRepository;

    @InjectMocks
    SessionStateServiceImpl service;

    UUID sessionId = UUID.randomUUID();
    Session session;

    @BeforeEach
    void setUp() {
        session = new Session("token", LocalDateTime.now().plusDays(7)); // CREATED
    }

    @Test
    void requireState_passesWhenStateAllowed() {
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));

        service.requireState(sessionId, SessionState.CREATED, SessionState.EXPLORING);
    }

    @Test
    void requireState_throws409WithCurrentState() {
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> service.requireState(sessionId, SessionState.DILEMMA))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException ex = (ApiException) e;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
                    assertThat(ex.getCurrentState()).isEqualTo("CREATED");
                });
    }

    @Test
    void requireState_throws404WhenSessionMissing() {
        given(sessionRepository.findById(sessionId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireState(sessionId, SessionState.CREATED))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void transition_updatesStateWhenAllowed() {
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(sessionRepository.updateStateIfMatches(sessionId, SessionState.CREATED, SessionState.EXPLORING))
                .willReturn(1);

        service.transition(sessionId, SessionState.EXPLORING);

        assertThat(session.getState()).isEqualTo(SessionState.EXPLORING);
    }

    @Test
    void transition_throws409WhenNotAllowed() {
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> service.transition(sessionId, SessionState.REPORT))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
        verify(sessionRepository, never()).updateStateIfMatches(any(), any(), any());
    }

    @Test
    void transition_sameStateIsNoOp() {
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));

        service.transition(sessionId, SessionState.CREATED);

        verify(sessionRepository, never()).updateStateIfMatches(any(), any(), any());
        assertThat(session.getState()).isEqualTo(SessionState.CREATED);
    }

    @Test
    void transition_throws409WhenAnotherRequestChangedStateFirst() {
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(sessionRepository.updateStateIfMatches(sessionId, SessionState.CREATED, SessionState.EXPLORING))
                .willReturn(0);

        assertThatThrownBy(() -> service.transition(sessionId, SessionState.EXPLORING))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
        assertThat(session.getState()).isEqualTo(SessionState.CREATED);
    }
}
