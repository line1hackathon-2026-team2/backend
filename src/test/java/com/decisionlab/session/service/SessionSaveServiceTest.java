package com.decisionlab.session.service;

import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

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

class SessionSaveServiceTest {

    private final SessionStateService sessionStateService = mock(SessionStateService.class);
    private final SessionRepository sessionRepository = mock(SessionRepository.class);
    private final ResumeCodeGenerator resumeCodeGenerator = mock(ResumeCodeGenerator.class);
    private final SessionSaveService service =
            new SessionSaveService(sessionStateService, sessionRepository, resumeCodeGenerator);

    private final UUID sessionId = UUID.randomUUID();
    private Session session;

    @BeforeEach
    void setUp() {
        session = new Session("token", LocalDateTime.now().plusDays(7));
        ReflectionTestUtils.setField(session, "id", sessionId);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
    }

    @Test
    void PAUSED로_바꾸고_새_코드를_발급한다() {
        when(resumeCodeGenerator.generate()).thenReturn("ABCD-1234");

        String code = service.save(sessionId);

        assertThat(code).isEqualTo("ABCD-1234");
        assertThat(session.getResumeCode()).isEqualTo("ABCD-1234");
        InOrder order = inOrder(sessionStateService, resumeCodeGenerator);
        order.verify(sessionStateService).requireState(sessionId, SessionState.EXPLORING);
        order.verify(sessionStateService).transition(sessionId, SessionState.PAUSED);
        order.verify(resumeCodeGenerator).generate();
    }

    @Test
    void 다시_저장하면_이전_코드가_새_코드로_교체된다() {
        session.assignResumeCode("OLDC-0000");
        when(resumeCodeGenerator.generate()).thenReturn("NEWC-1111");

        service.save(sessionId);

        assertThat(session.getResumeCode()).isEqualTo("NEWC-1111");
    }

    @Test
    void 이미_쓰는_코드가_나오면_다시_생성한다() {
        when(resumeCodeGenerator.generate()).thenReturn("DUPL-0000", "UNIQ-1111");
        when(sessionRepository.existsByResumeCode("DUPL-0000")).thenReturn(true);

        assertThat(service.save(sessionId)).isEqualTo("UNIQ-1111");
    }

    @Test
    void 계속_겹치면_무한히_반복하지_않고_실패한다() {
        when(resumeCodeGenerator.generate()).thenReturn("DUPL-0000");
        when(sessionRepository.existsByResumeCode("DUPL-0000")).thenReturn(true);

        assertThatThrownBy(() -> service.save(sessionId)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void EXPLORING이_아니면_409이고_코드를_발급하지_않는다() {
        doThrow(new ApiException(ErrorCode.INVALID_STATE_TRANSITION, "불가", "REPORT"))
                .when(sessionStateService).requireState(sessionId, SessionState.EXPLORING);

        assertThatThrownBy(() -> service.save(sessionId))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);

        verify(sessionStateService, never()).transition(any(), any());
        verify(resumeCodeGenerator, never()).generate();
        assertThat(session.getResumeCode()).isNull();
    }

    @Test
    void 동시에_저장해_늦게_처리되면_409이고_코드를_바꾸지_않는다() {
        session.assignResumeCode("FRST-0001");
        doThrow(new ApiException(ErrorCode.INVALID_STATE_TRANSITION, "다른 요청이 먼저 처리됨", "EXPLORING"))
                .when(sessionStateService).transition(sessionId, SessionState.PAUSED);

        assertThatThrownBy(() -> service.save(sessionId)).isInstanceOf(ApiException.class);

        verify(resumeCodeGenerator, never()).generate();
        assertThat(session.getResumeCode()).isEqualTo("FRST-0001");
    }
}
