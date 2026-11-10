package com.decisionlab.persona.controller;

import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.persona.dto.PersonaSelectRequest;
import com.decisionlab.persona.dto.PersonaSelectResponse;
import com.decisionlab.persona.dto.PersonaSelectResponse.OpeningMessage;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.persona.entity.SwitchReason;
import com.decisionlab.persona.service.PersonaSelectService;
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
import java.util.List;
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

@WebMvcTest(controllers = PersonaSelectController.class)
class PersonaSelectControllerTest {

    private static final String TOKEN = "token";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PersonaSelectService personaSelectService;

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

    private ResultActions postPersona(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/sessions/{id}/persona", sessionId)
                .header("X-Session-Token", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void 명세서_4_5_형식으로_200() throws Exception {
        when(personaSelectService.selectPersona(eq(sessionId), any(PersonaSelectRequest.class)))
                .thenReturn(new PersonaSelectResponse(SessionState.EXPLORING, PersonaCode.STABILITY,
                        new OpeningMessage("42", "현실적인 부담이 신경 쓰이기 시작한 거야?", List.of(
                                new MessageOption("o1", "맞아"),
                                new MessageOption("o2", "조금 달라")))));

        postPersona("""
                {"personaCode":"STABILITY","reason":"SWITCH"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("EXPLORING"))
                .andExpect(jsonPath("$.personaCode").value("STABILITY"))
                .andExpect(jsonPath("$.openingMessage.messageId").value("42"))
                .andExpect(jsonPath("$.openingMessage.content").value("현실적인 부담이 신경 쓰이기 시작한 거야?"))
                .andExpect(jsonPath("$.openingMessage.options[0].optionId").value("o1"))
                .andExpect(jsonPath("$.openingMessage.options[0].label").value("맞아"))
                .andExpect(jsonPath("$.openingMessage.options.length()").value(2));

        verify(personaSelectService).selectPersona(sessionId,
                new PersonaSelectRequest(PersonaCode.STABILITY, SwitchReason.SWITCH));
    }

    @Test
    void personaCode가_없으면_400() throws Exception {
        postPersona("""
                {"reason":"USER_SELECT"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(personaSelectService, never()).selectPersona(any(), any());
    }

    @Test
    void reason이_없으면_400() throws Exception {
        postPersona("""
                {"personaCode":"ACHIEVEMENT"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 상태가_맞지_않으면_409와_현재상태() throws Exception {
        when(personaSelectService.selectPersona(eq(sessionId), any(PersonaSelectRequest.class)))
                .thenThrow(new ApiException(ErrorCode.INVALID_STATE_TRANSITION,
                        "현재 상태(DILEMMA)에서는 이 요청을 처리할 수 없습니다.", "DILEMMA"));

        postPersona("""
                {"personaCode":"STABILITY","reason":"SWITCH"}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"))
                .andExpect(jsonPath("$.error.currentState").value("DILEMMA"));
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{id}/persona", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personaCode\":\"ACHIEVEMENT\",\"reason\":\"USER_SELECT\"}"))
                .andExpect(status().isUnauthorized());
    }
}
