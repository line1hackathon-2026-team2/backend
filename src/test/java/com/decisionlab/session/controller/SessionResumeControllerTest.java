package com.decisionlab.session.controller;

import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.session.dto.ResumeRequest;
import com.decisionlab.session.dto.ResumeResponse;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.repository.SessionRepository;
import com.decisionlab.session.service.SessionResumeService;
import com.decisionlab.session.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SessionResumeController.class)
class SessionResumeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SessionResumeService sessionResumeService;

    // SessionTokenFilter 의존성
    @MockitoBean
    SessionRepository sessionRepository;

    @MockitoBean
    SessionService sessionService;

    private ResultActions postResume(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/sessions/resume")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void 토큰_없이_호출하고_새_토큰과_EXPLORING을_돌려준다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(sessionResumeService.resume(new ResumeRequest("ABCD-1234")))
                .thenReturn(new ResumeResponse(sessionId, "new-token", SessionState.EXPLORING));

        postResume("{\"resumeCode\":\"ABCD-1234\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.sessionToken").value("new-token"))
                .andExpect(jsonPath("$.state").value("EXPLORING"));
    }

    @Test
    void 코드가_비어_있으면_400() throws Exception {
        postResume("{\"resumeCode\":\" \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(sessionResumeService, never()).resume(any());
    }

    @Test
    void 없는_코드면_404() throws Exception {
        when(sessionResumeService.resume(any()))
                .thenThrow(new ApiException(ErrorCode.NOT_FOUND, "이어하기 코드를 찾을 수 없습니다."));

        postResume("{\"resumeCode\":\"ZZZZ-9999\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void 이미_이어간_코드면_409와_현재상태() throws Exception {
        when(sessionResumeService.resume(any()))
                .thenThrow(new ApiException(ErrorCode.INVALID_STATE_TRANSITION, "불가", "EXPLORING"));

        postResume("{\"resumeCode\":\"ABCD-1234\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.currentState").value("EXPLORING"));
    }
}
