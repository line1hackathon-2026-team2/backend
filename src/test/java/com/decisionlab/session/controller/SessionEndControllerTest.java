package com.decisionlab.session.controller;

import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.session.entity.EndMode;
import com.decisionlab.session.dto.EndRequest;
import com.decisionlab.session.dto.EndResponse;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.repository.SessionRepository;
import com.decisionlab.session.service.SessionEndService;
import com.decisionlab.session.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SessionEndController.class)
class SessionEndControllerTest {

    private static final String TOKEN = "token";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SessionEndService sessionEndService;

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

    private ResultActions postEnd(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/sessions/{id}/end", sessionId)
                .header("X-Session-Token", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void 명세서_4_13_형식으로_200() throws Exception {
        when(sessionEndService.end(sessionId, new EndRequest(EndMode.INCOMPLETE_REPORT)))
                .thenReturn(EndResponse.report(15L));

        postEnd("{\"mode\":\"INCOMPLETE_REPORT\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("REPORT"))
                .andExpect(jsonPath("$.reportId").value("15"))
                .andExpect(jsonPath("$.resumeCode").doesNotExist());
    }

    @Test
    void SAVE면_명세서_4_13_형식으로_PAUSED와_resumeCode만_내려준다() throws Exception {
        when(sessionEndService.end(sessionId, new EndRequest(EndMode.SAVE)))
                .thenReturn(EndResponse.paused("ABCD-1234"));

        postEnd("{\"mode\":\"SAVE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PAUSED"))
                .andExpect(jsonPath("$.resumeCode").value("ABCD-1234"))
                .andExpect(jsonPath("$.reportId").doesNotExist());
    }

    @Test
    void mode가_없으면_400() throws Exception {
        postEnd("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(sessionEndService, never()).end(any(), any());
    }

    @Test
    void mode가_enum에_없는_값이면_400() throws Exception {
        postEnd("{\"mode\":\"QUIT\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(sessionEndService, never()).end(any(), any());
    }

    @Test
    void EXPLORING이_아니면_409와_현재상태() throws Exception {
        when(sessionEndService.end(eq(sessionId), any(EndRequest.class)))
                .thenThrow(new ApiException(ErrorCode.INVALID_STATE_TRANSITION, "불가", "REPORT"));

        postEnd("{\"mode\":\"INCOMPLETE_REPORT\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"))
                .andExpect(jsonPath("$.error.currentState").value("REPORT"));
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{id}/end", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"INCOMPLETE_REPORT\"}"))
                .andExpect(status().isUnauthorized());
        verify(sessionEndService, never()).end(any(), any());
    }
}
