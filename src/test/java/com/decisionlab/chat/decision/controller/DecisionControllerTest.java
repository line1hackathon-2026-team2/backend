package com.decisionlab.chat.decision.controller;

import com.decisionlab.chat.decision.dto.ResolveResponse;
import com.decisionlab.dilemma.dto.DilemmaResult;
import com.decisionlab.dilemma.entity.DilemmaType;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.repository.SessionRepository;
import com.decisionlab.session.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DecisionController.class)
class DecisionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    com.decisionlab.chat.decision.service.DecisionService decisionService;

    // SessionTokenFilter 의존성
    @MockitoBean
    SessionRepository sessionRepository;

    @MockitoBean
    SessionService sessionService;

    private Session session(UUID id) {
        Session session = new Session("token", LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }

    @Test
    void 승인_첫_시도는_명세서_4_10_딜레마_응답_형식이다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(sessionRepository.findBySessionToken("token")).willReturn(Optional.of(session(sessionId)));
        DilemmaResult dilemmaResult = new DilemmaResult(1L, DilemmaType.ALT_PATH, "시나리오", "검증 목표", false);
        given(decisionService.resolve(eq(sessionId), eq(10L), any()))
                .willReturn(ResolveResponse.toDilemma(dilemmaResult));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/decision/{decisionId}/resolve", sessionId, 10L)
                        .header("X-Session-Token", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"APPROVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("DILEMMA"))
                .andExpect(jsonPath("$.dilemma.dilemmaId").value("1"))
                .andExpect(jsonPath("$.dilemma.scenarioText").value("시나리오"))
                .andExpect(jsonPath("$.reportId").doesNotExist());
    }

    @Test
    void 미결정은_명세서_4_10_리포트_응답_형식이다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(sessionRepository.findBySessionToken("token")).willReturn(Optional.of(session(sessionId)));
        given(decisionService.resolve(eq(sessionId), eq(10L), any()))
                .willReturn(ResolveResponse.toReport(77L));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/decision/{decisionId}/resolve", sessionId, 10L)
                        .header("X-Session-Token", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"UNDECIDED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("REPORT"))
                .andExpect(jsonPath("$.reportId").value("77"));
    }

    @Test
    void 수정은_EXPLORING으로만_응답한다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(sessionRepository.findBySessionToken("token")).willReturn(Optional.of(session(sessionId)));
        given(decisionService.resolve(eq(sessionId), eq(10L), any()))
                .willReturn(ResolveResponse.toExploring());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/decision/{decisionId}/resolve", sessionId, 10L)
                        .header("X-Session-Token", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"REVISE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("EXPLORING"));
    }
}
