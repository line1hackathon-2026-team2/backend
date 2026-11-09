package com.decisionlab.chat.decision.controller;

import com.decisionlab.chat.decision.dto.EditFieldResponse;
import com.decisionlab.chat.decision.dto.EditFieldResponse.Fields;
import com.decisionlab.chat.decision.service.DecisionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DecisionController.class)
class DecisionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DecisionService decisionService;

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
    void 필드_수정은_명세서_4_9_형식으로_응답한다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(sessionRepository.findBySessionToken("token")).willReturn(Optional.of(session(sessionId)));
        given(decisionService.editField(eq(sessionId), eq(10L), any())).willReturn(new EditFieldResponse(
                "10", 1,
                new Fields("대학원 진학", "경제적 안정", "연구개발 분야 희망", "취업 시점 지연", "실제 연구 경험")
        ));

        mockMvc.perform(patch("/api/v1/sessions/{sessionId}/decision/{decisionId}", sessionId, 10L)
                        .header("X-Session-Token", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"field":"coreValue","value":"경제적 안정"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisionId").value("10"))
                .andExpect(jsonPath("$.revisionNo").value(1))
                .andExpect(jsonPath("$.fields.coreValue").value("경제적 안정"))
                .andExpect(jsonPath("$.fields.decisionText").value("대학원 진학"));
    }

    @Test
    void value가_없으면_400을_응답한다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(sessionRepository.findBySessionToken("token")).willReturn(Optional.of(session(sessionId)));

        mockMvc.perform(patch("/api/v1/sessions/{sessionId}/decision/{decisionId}", sessionId, 10L)
                        .header("X-Session-Token", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"field":"coreValue","value":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
