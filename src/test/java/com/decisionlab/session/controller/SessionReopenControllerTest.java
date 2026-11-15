package com.decisionlab.session.controller;

import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.session.dto.ReopenResponse;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.repository.SessionRepository;
import com.decisionlab.session.service.SessionReopenService;
import com.decisionlab.session.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SessionReopenController.class)
class SessionReopenControllerTest {

    private static final String TOKEN = "token";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SessionReopenService sessionReopenService;

    // SessionTokenFilter 의존성
    @MockitoBean
    SessionRepository sessionRepository;

    @MockitoBean
    SessionService sessionService;

    private final UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void validToken() {
        Session session = new Session(TOKEN, LocalDateTime.now().plusDays(7));
        ReflectionTestUtils.setField(session, "id", sessionId);
        when(sessionRepository.findBySessionToken(TOKEN)).thenReturn(Optional.of(session));
    }

    @Test
    void 본문_없이_호출하면_명세서_4_16_형식으로_200() throws Exception {
        when(sessionReopenService.reopen(sessionId)).thenReturn(ReopenResponse.exploring());

        mockMvc.perform(post("/api/v1/sessions/{id}/reopen", sessionId).header("X-Session-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("EXPLORING"));
    }

    @Test
    void REPORT가_아니면_409와_현재상태() throws Exception {
        when(sessionReopenService.reopen(sessionId))
                .thenThrow(new ApiException(ErrorCode.INVALID_STATE_TRANSITION, "불가", "EXPLORING"));

        mockMvc.perform(post("/api/v1/sessions/{id}/reopen", sessionId).header("X-Session-Token", TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"))
                .andExpect(jsonPath("$.error.currentState").value("EXPLORING"));
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{id}/reopen", sessionId))
                .andExpect(status().isUnauthorized());
        verify(sessionReopenService, never()).reopen(any());
    }

    @Test
    void 다른_세션이면_403() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{id}/reopen", UUID.randomUUID()).header("X-Session-Token", TOKEN))
                .andExpect(status().isForbidden());
        verify(sessionReopenService, never()).reopen(any());
    }
}
