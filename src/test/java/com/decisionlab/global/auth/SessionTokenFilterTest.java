package com.decisionlab.global.auth;

import com.decisionlab.session.controller.SessionController;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.repository.SessionRepository;
import com.decisionlab.session.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.decisionlab.session.dto.SessionCreateResponse;
import com.decisionlab.session.entity.SessionState;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SessionController.class)
@Import(SessionTokenFilterTest.PingController.class)
class SessionTokenFilterTest {

    private static final String HEADER = "X-Session-Token";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SessionRepository sessionRepository;

    @MockitoBean
    SessionService sessionService;

    /** 필터 통과 여부 확인용 테스트 전용 엔드포인트 */
    @RestController
    static class PingController {
        @GetMapping("/api/v1/sessions/{id}/ping")
        String ping(@PathVariable UUID id) {
            return SessionContext.getSessionId().toString();
        }

        @GetMapping("/api/v1/reports/{reportId}")
        String publicReport(@PathVariable String reportId) {
            return "report-" + reportId;
        }
    }

    private Session session(UUID id, LocalDateTime expiresAt) {
        Session session = new Session("token", expiresAt);
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/{id}/ping", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 존재하지_않는_토큰이면_401() throws Exception {
        when(sessionRepository.findBySessionToken("bad")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/sessions/{id}/ping", UUID.randomUUID()).header(HEADER, "bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 만료된_세션이면_410과_현재상태() throws Exception {
        UUID id = UUID.randomUUID();
        when(sessionRepository.findBySessionToken("token"))
                .thenReturn(Optional.of(session(id, LocalDateTime.now().minusMinutes(1))));

        mockMvc.perform(get("/api/v1/sessions/{id}/ping", id).header(HEADER, "token"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error.code").value("SESSION_EXPIRED"))
                .andExpect(jsonPath("$.error.currentState").value("CREATED"));
    }

    @Test
    void 다른_세션_id로_접근하면_403() throws Exception {
        when(sessionRepository.findBySessionToken("token"))
                .thenReturn(Optional.of(session(UUID.randomUUID(), LocalDateTime.now().plusDays(1))));

        mockMvc.perform(get("/api/v1/sessions/{id}/ping", UUID.randomUUID()).header(HEADER, "token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void 유효한_토큰이면_통과하고_SessionContext에_id가_담긴다() throws Exception {
        UUID id = UUID.randomUUID();
        when(sessionRepository.findBySessionToken("token"))
                .thenReturn(Optional.of(session(id, LocalDateTime.now().plusDays(1))));

        mockMvc.perform(get("/api/v1/sessions/{id}/ping", id).header(HEADER, "token"))
                .andExpect(status().isOk())
                .andExpect(content().string(id.toString()));
        verify(sessionService).renewExpiry(eq(id), any());
    }

    @Test
    void 인증에_실패하면_만료를_연장하지_않는다() throws Exception {
        UUID id = UUID.randomUUID();
        when(sessionRepository.findBySessionToken("token"))
                .thenReturn(Optional.of(session(id, LocalDateTime.now().minusMinutes(1))));

        mockMvc.perform(get("/api/v1/sessions/{id}/ping", id).header(HEADER, "token"))
                .andExpect(status().isGone());
        verify(sessionService, never()).renewExpiry(any(), any());
    }

    @Test
    void 리포트_공개_조회는_토큰_없이_통과한다() throws Exception {
        mockMvc.perform(get("/api/v1/reports/{reportId}", "12"))
                .andExpect(status().isOk())
                .andExpect(content().string("report-12"));
        verify(sessionRepository, never()).findBySessionToken(any());
        verify(sessionService, never()).renewExpiry(any(), any());
    }

    @Test
    void 공개_예외는_GET_reports_단건뿐이고_나머지는_토큰이_필요하다() throws Exception {
        mockMvc.perform(post("/api/v1/reports/{reportId}", "12"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/reports/{reportId}/extra", "12"))
                .andExpect(status().isUnauthorized());
        // #14 세션 리포트 조회는 토큰 필요
        mockMvc.perform(get("/api/v1/sessions/{id}/report", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 세션_생성은_토큰_없이_201이고_명세서_4_1_형식으로_응답한다() throws Exception {
        UUID id = UUID.randomUUID();
        when(sessionService.createSession()).thenReturn(new SessionCreateResponse(
                id, "token", SessionState.CREATED,
                OffsetDateTime.of(2026, 11, 9, 21, 0, 0, 0, ZoneOffset.ofHours(9))));

        mockMvc.perform(post("/api/v1/sessions"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(id.toString()))
                .andExpect(jsonPath("$.sessionToken").value("token"))
                .andExpect(jsonPath("$.state").value("CREATED"))
                .andExpect(jsonPath("$.expiresAt").value("2026-11-09T21:00:00+09:00"));
    }
}
