package com.decisionlab.dilemma.controller;

import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.dilemma.dto.DilemmaFollowupRequest;
import com.decisionlab.dilemma.dto.DilemmaFollowupResponse;
import com.decisionlab.dilemma.dto.DilemmaRespondRequest;
import com.decisionlab.dilemma.dto.DilemmaRespondResponse;
import com.decisionlab.dilemma.entity.DilemmaChoice;
import com.decisionlab.dilemma.service.DilemmaService;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.repository.SessionRepository;
import com.decisionlab.session.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DilemmaController.class)
class DilemmaControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DilemmaService dilemmaService;

    @MockitoBean
    SessionRepository sessionRepository;

    @MockitoBean
    SessionService sessionService;

    private final UUID sessionId = UUID.randomUUID();
    private final String validToken = "valid-session-token";

    @BeforeEach
    void setUp() throws Exception {
        Session session = new Session(validToken, LocalDateTime.now().plusDays(7));
        Field idField = Session.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(session, sessionId);

        when(sessionRepository.findBySessionToken(validToken)).thenReturn(Optional.of(session));
    }

    @Test
    void KEEP_선택_시_REFLECTION_상태와_프롬프트_및_선택지를_반환한다() throws Exception {
        when(dilemmaService.respond(sessionId, new DilemmaRespondRequest(DilemmaChoice.KEEP)))
                .thenReturn(DilemmaRespondResponse.keep(
                        "새로운 조건이 생겼는데도 같은 선택을 유지하는 이유가 무엇인가요?",
                        List.of(new MessageOption("o1", "처음 중요하게 생각한 가치가 여전히 가장 커요"))
                ));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/dilemma/respond", sessionId)
                        .header("X-Session-Token", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "choice": "KEEP" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("REFLECTION"))
                .andExpect(jsonPath("$.prompt").value("새로운 조건이 생겼는데도 같은 선택을 유지하는 이유가 무엇인가요?"))
                .andExpect(jsonPath("$.options[0].optionId").value("o1"))
                .andExpect(jsonPath("$.options[0].label").value("처음 중요하게 생각한 가치가 여전히 가장 커요"));
    }

    @Test
    void RECONSIDER_선택_시_RECONSIDER_상태와_선택지_및_추천_자아를_반환한다() throws Exception {
        when(dilemmaService.respond(sessionId, new DilemmaRespondRequest(DilemmaChoice.RECONSIDER)))
                .thenReturn(DilemmaRespondResponse.reconsider(
                        "어떤 부분이 마음을 흔들었나요?",
                        List.of(new MessageOption("o1", "새로운 조건에서 제시된 기회를 포기하는 게 아까워졌어요")),
                        PersonaCode.STABILITY
                ));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/dilemma/respond", sessionId)
                        .header("X-Session-Token", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "choice": "RECONSIDER" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("RECONSIDER"))
                .andExpect(jsonPath("$.prompt").value("어떤 부분이 마음을 흔들었나요?"))
                .andExpect(jsonPath("$.options[0].optionId").value("o1"))
                .andExpect(jsonPath("$.suggestedPersonaCode").value("STABILITY"));
    }

    @Test
    void UNDECIDED_선택_시_REPORT_상태와_reportId를_반환한다() throws Exception {
        when(dilemmaService.respond(sessionId, new DilemmaRespondRequest(DilemmaChoice.UNDECIDED)))
                .thenReturn(DilemmaRespondResponse.undecided(5L));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/dilemma/respond", sessionId)
                        .header("X-Session-Token", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "choice": "UNDECIDED" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("REPORT"))
                .andExpect(jsonPath("$.reportId").value("5"));
    }

    @Test
    void choice가_누락되면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/dilemma/respond", sessionId)
                        .header("X-Session-Token", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 재검토_한도_초과_시_409_RECONSIDER_LIMIT을_반환한다() throws Exception {
        when(dilemmaService.respond(sessionId, new DilemmaRespondRequest(DilemmaChoice.RECONSIDER)))
                .thenThrow(new ApiException(ErrorCode.RECONSIDER_LIMIT, "재검토는 세션당 1회만 가능합니다."));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/dilemma/respond", sessionId)
                        .header("X-Session-Token", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "choice": "RECONSIDER" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RECONSIDER_LIMIT"));
    }

    @Test
    void keepReason_입력_시_REPORT_상태와_reportId를_반환한다() throws Exception {
        when(dilemmaService.followup(sessionId,
                new DilemmaFollowupRequest("체계적으로 연구 방법을 배우는 과정이 더 중요하다고 생각해요.", null)))
                .thenReturn(DilemmaFollowupResponse.keep(15L));

        mockMvc.perform(put("/api/v1/sessions/{sessionId}/dilemma/followup", sessionId)
                        .header("X-Session-Token", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "keepReason": "체계적으로 연구 방법을 배우는 과정이 더 중요하다고 생각해요." }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("REPORT"))
                .andExpect(jsonPath("$.reportId").value("15"));
    }

    @Test
    void shakenValue_입력_시_EXPLORING_상태와_reconsiderCount_및_추천_자아를_반환한다() throws Exception {
        when(dilemmaService.followup(sessionId,
                new DilemmaFollowupRequest(null, "좋은 기업에서 일할 기회를 포기하는 게 아까워졌어요.")))
                .thenReturn(DilemmaFollowupResponse.reconsider(1, PersonaCode.STABILITY));

        mockMvc.perform(put("/api/v1/sessions/{sessionId}/dilemma/followup", sessionId)
                        .header("X-Session-Token", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "shakenValue": "좋은 기업에서 일할 기회를 포기하는 게 아까워졌어요." }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("EXPLORING"))
                .andExpect(jsonPath("$.reconsiderCount").value(1))
                .andExpect(jsonPath("$.suggestedPersonaCode").value("STABILITY"));
    }

    @Test
    void keepReason이_500자를_초과하면_400_VALIDATION_ERROR를_반환한다() throws Exception {
        String over500 = "가".repeat(501);

        mockMvc.perform(put("/api/v1/sessions/{sessionId}/dilemma/followup", sessionId)
                        .header("X-Session-Token", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keepReason\": \"" + over500 + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
