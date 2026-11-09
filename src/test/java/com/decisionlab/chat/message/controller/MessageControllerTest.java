package com.decisionlab.chat.message.controller;

import com.decisionlab.chat.message.dto.MessageHistoryResponse;
import com.decisionlab.chat.message.dto.MessageHistoryResponse.MessageItem;
import com.decisionlab.chat.message.dto.MessageResponse;
import com.decisionlab.chat.message.dto.MessageResponse.AiMessageItem;
import com.decisionlab.chat.message.entity.MessageRole;
import com.decisionlab.chat.message.service.MessageService;
import com.decisionlab.persona.entity.PersonaCode;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MessageController.class)
class MessageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MessageService messageService;

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
    void 메시지_전송은_명세서_4_6_형식으로_응답한다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(sessionRepository.findBySessionToken("token")).willReturn(Optional.of(session(sessionId)));
        given(messageService.send(eq(sessionId), any())).willReturn(new MessageResponse(
                "1",
                new AiMessageItem("2", PersonaCode.ACHIEVEMENT, "연구 역량이 왜 중요한지 조금 더 말해줄래?", List.of()),
                3,
                true
        ));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/messages", sessionId)
                        .header("X-Session-Token", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inputType":"FREE_TEXT","content":"장기적으로 연구개발을 하고 싶어서"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userMessageId").value("1"))
                .andExpect(jsonPath("$.aiMessage.messageId").value("2"))
                .andExpect(jsonPath("$.turnNo").value(3))
                .andExpect(jsonPath("$.canProceedToDecision").value(true));
    }

    @Test
    void 대화_이력_조회는_명세서_4_7_형식으로_응답한다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(sessionRepository.findBySessionToken("token")).willReturn(Optional.of(session(sessionId)));
        given(messageService.getHistory(eq(sessionId), any(), any())).willReturn(new MessageHistoryResponse(List.of(
                new MessageItem("1", MessageRole.AI, PersonaCode.ACHIEVEMENT, "...", List.of(), null, 1,
                        OffsetDateTime.now())
        )));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/messages", sessionId)
                        .header("X-Session-Token", "token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].messageId").value("1"))
                .andExpect(jsonPath("$.messages[0].role").value("AI"));
    }
}
