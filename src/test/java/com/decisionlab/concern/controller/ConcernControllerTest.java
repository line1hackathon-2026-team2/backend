package com.decisionlab.concern.controller;

import com.decisionlab.concern.dto.ConcernRequest;
import com.decisionlab.concern.dto.ConcernResponse;
import com.decisionlab.concern.entity.ConcernType;
import com.decisionlab.concern.service.ConcernService;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.repository.SessionRepository;
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

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConcernController.class)
class ConcernControllerTest {

    private static final String TOKEN = "token";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ConcernService concernService;

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

    private ResultActions putConcern(String body) throws Exception {
        return mockMvc.perform(put("/api/v1/sessions/{id}/concern", sessionId)
                .header("X-Session-Token", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void 명세서_4_3_형식으로_200() throws Exception {
        when(concernService.saveConcern(eq(sessionId), any(ConcernRequest.class)))
                .thenReturn(ConcernResponse.of(SessionState.CREATED, 12L));

        putConcern("""
                {"freeText":"취업과 대학원 진학 사이에서 고민 중이에요.","concernType":"CAREER",
                 "options":["취업","대학원 진학"],"initialPreference":"취업",
                 "initialPreferenceReason":"전문성을 더 키우고 싶어"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CREATED"))
                .andExpect(jsonPath("$.concernId").value("12"));

        ArgumentCaptor<ConcernRequest> captor = ArgumentCaptor.forClass(ConcernRequest.class);
        verify(concernService).saveConcern(eq(sessionId), captor.capture());
        assertThat(captor.getValue().options()).containsExactly("취업", "대학원 진학");
        assertThat(captor.getValue().initialPreference()).isEqualTo("취업");
        assertThat(captor.getValue().initialPreferenceReason()).isEqualTo("전문성을 더 키우고 싶어");
    }

    @Test
    void 선택지가_15자를_넘으면_400() throws Exception {
        putConcern("""
                {"freeText":"고민","concernType":"CAREER","options":["가나다라마바사아자차카타파하가나","B"]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(concernService, never()).saveConcern(any(), any());
    }

    @Test
    void 선택지가_빈_문자열이면_400() throws Exception {
        putConcern("""
                {"freeText":"고민","concernType":"CAREER","options":["취업"," "]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 선택한_이유가_100자를_넘으면_400() throws Exception {
        String tooLong = "가".repeat(101);

        putConcern("{\"freeText\":\"고민\",\"concernType\":\"CAREER\",\"initialPreferenceReason\":\"" + tooLong + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 초기_선호는_없어도_된다() throws Exception {
        when(concernService.saveConcern(eq(sessionId), any(ConcernRequest.class)))
                .thenReturn(ConcernResponse.of(SessionState.CREATED, 1L));

        putConcern("""
                {"freeText":"고민","concernType":"OTHER"}
                """)
                .andExpect(status().isOk());
    }

    @Test
    void freeText가_없으면_400() throws Exception {
        putConcern("""
                {"concernType":"CAREER"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(concernService, never()).saveConcern(any(), any());
    }

    @Test
    void freeText가_1000자를_넘으면_400() throws Exception {
        String tooLong = "가".repeat(1001);

        putConcern("{\"freeText\":\"" + tooLong + "\",\"concernType\":\"CAREER\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void concernType이_없으면_400() throws Exception {
        putConcern("""
                {"freeText":"고민"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void concernType이_enum에_없는_값이면_400() throws Exception {
        putConcern("""
                {"freeText":"고민","concernType":"JOB"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void CREATED가_아니면_409와_현재상태() throws Exception {
        when(concernService.saveConcern(eq(sessionId), any(ConcernRequest.class)))
                .thenThrow(new ApiException(ErrorCode.INVALID_STATE_TRANSITION,
                        "현재 상태(EXPLORING)에서는 이 요청을 처리할 수 없습니다.", "EXPLORING"));

        putConcern("""
                {"freeText":"고민","concernType":"CAREER"}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"))
                .andExpect(jsonPath("$.error.currentState").value("EXPLORING"));
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(put("/api/v1/sessions/{id}/concern", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"freeText\":\"고민\",\"concernType\":\"CAREER\"}"))
                .andExpect(status().isUnauthorized());
    }
}
