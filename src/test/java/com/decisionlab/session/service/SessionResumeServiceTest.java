package com.decisionlab.session.service;

import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.session.dto.ResumeRequest;
import com.decisionlab.session.dto.ResumeResponse;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionResumeServiceTest {

    private final SessionRepository sessionRepository = mock(SessionRepository.class);
    private final SessionStateService sessionStateService = mock(SessionStateService.class);
    private final SessionTokenGenerator tokenGenerator = mock(SessionTokenGenerator.class);
    private final SessionResumeService service = new SessionResumeService(
            sessionRepository, sessionStateService, tokenGenerator, Duration.ofDays(7));

    private final UUID sessionId = UUID.randomUUID();
    private Session session;

    @BeforeEach
    void setUp() {
        session = new Session("old-token", LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(session, "id", sessionId);
        session.transitionTo(SessionState.PAUSED);
        session.assignResumeCode("ABCD-1234");
        when(sessionRepository.findByResumeCode("ABCD-1234")).thenReturn(Optional.of(session));
        when(tokenGenerator.generate()).thenReturn("new-token");
    }

    @Test
    void 새_토큰을_발급하고_바로_EXPLORING으로_돌려준다() {
        ResumeResponse response = service.resume(new ResumeRequest("ABCD-1234"));

        assertThat(response.sessionId()).isEqualTo(sessionId);
        assertThat(response.sessionToken()).isEqualTo("new-token");
        assertThat(response.state()).isEqualTo(SessionState.EXPLORING);

        // 이전 토큰은 교체되어 끊기고, 만료는 지금부터 7일로 연장된다
        assertThat(session.getSessionToken()).isEqualTo("new-token");
        assertThat(session.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(6));

        InOrder order = inOrder(sessionStateService, tokenGenerator);
        order.verify(sessionStateService).requireState(sessionId, SessionState.PAUSED);
        order.verify(sessionStateService).transition(sessionId, SessionState.EXPLORING);
        order.verify(tokenGenerator).generate();
    }

    @Test
    void 이어간_뒤에도_코드는_다음_SAVE_전까지_그대로_남는다() {
        service.resume(new ResumeRequest("ABCD-1234"));

        assertThat(session.getResumeCode()).isEqualTo("ABCD-1234");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd-1234", " ABCD-1234 ", "abcd1234", "ABCD1234"})
    void 소문자_공백_하이픈_생략도_받아준다(String input) {
        assertThat(service.resume(new ResumeRequest(input)).sessionToken()).isEqualTo("new-token");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC-1234", "ABCD-123", "1234-ABCD", "ABCD_1234", "가나다라-1234"})
    void 형식이_틀리면_400이고_조회하지_않는다(String input) {
        assertThatThrownBy(() -> service.resume(new ResumeRequest(input)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(sessionRepository, never()).findByResumeCode(any());
    }

    @Test
    void 없는_코드면_404() {
        assertThatThrownBy(() -> service.resume(new ResumeRequest("ZZZZ-9999")))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 만료된_세션이면_410과_현재상태() {
        ReflectionTestUtils.setField(session, "expiresAt", LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> service.resume(new ResumeRequest("ABCD-1234")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException api = (ApiException) e;
                    assertThat(api.getErrorCode()).isEqualTo(ErrorCode.SESSION_EXPIRED);
                    assertThat(api.getCurrentState()).isEqualTo("PAUSED");
                });
        verify(sessionStateService, never()).transition(any(), any());
        assertThat(session.getSessionToken()).isEqualTo("old-token");
    }

    @Test
    void 이미_이어간_코드면_409이고_토큰을_바꾸지_않는다() {
        doThrow(new ApiException(ErrorCode.INVALID_STATE_TRANSITION, "불가", "EXPLORING"))
                .when(sessionStateService).requireState(sessionId, SessionState.PAUSED);

        assertThatThrownBy(() -> service.resume(new ResumeRequest("ABCD-1234")))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
        verify(tokenGenerator, never()).generate();
        assertThat(session.getSessionToken()).isEqualTo("old-token");
    }

    @Test
    void 같은_코드로_동시에_이어가_늦게_처리되면_409이고_토큰을_바꾸지_않는다() {
        doThrow(new ApiException(ErrorCode.INVALID_STATE_TRANSITION, "다른 요청이 먼저 처리됨", "PAUSED"))
                .when(sessionStateService).transition(sessionId, SessionState.EXPLORING);

        assertThatThrownBy(() -> service.resume(new ResumeRequest("ABCD-1234"))).isInstanceOf(ApiException.class);
        verify(tokenGenerator, never()).generate();
        assertThat(session.getSessionToken()).isEqualTo("old-token");
    }
}
