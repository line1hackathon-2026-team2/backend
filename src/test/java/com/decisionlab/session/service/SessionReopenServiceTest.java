package com.decisionlab.session.service;

import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.session.dto.ReopenResponse;
import com.decisionlab.session.entity.SessionState;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SessionReopenServiceTest {

    private final SessionStateService sessionStateService = mock(SessionStateService.class);
    private final SessionReopenService sessionReopenService = new SessionReopenService(sessionStateService);

    private final UUID sessionId = UUID.randomUUID();

    @Test
    void REPORT에서_EXPLORING으로_되돌린다() {
        ReopenResponse response = sessionReopenService.reopen(sessionId);

        assertThat(response.state()).isEqualTo(SessionState.EXPLORING);

        InOrder order = inOrder(sessionStateService);
        order.verify(sessionStateService).requireState(sessionId, SessionState.REPORT);
        order.verify(sessionStateService).transition(sessionId, SessionState.EXPLORING);
    }

    @Test
    void REPORT가_아니면_409이고_상태를_바꾸지_않는다() {
        doThrow(new ApiException(ErrorCode.INVALID_STATE_TRANSITION, "불가", "EXPLORING"))
                .when(sessionStateService).requireState(sessionId, SessionState.REPORT);

        assertThatThrownBy(() -> sessionReopenService.reopen(sessionId))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);

        verify(sessionStateService, never()).transition(any(), any());
    }

    @Test
    void 동시에_두_번_요청해_먼저_바뀌었으면_409() {
        doThrow(new ApiException(ErrorCode.INVALID_STATE_TRANSITION, "다른 요청이 먼저 처리됨", "REPORT"))
                .when(sessionStateService).transition(sessionId, SessionState.EXPLORING);

        assertThatThrownBy(() -> sessionReopenService.reopen(sessionId))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
    }
}
