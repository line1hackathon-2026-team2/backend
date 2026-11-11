package com.decisionlab.session.controller;

import com.decisionlab.chat.criterion.entity.CriterionSource;
import com.decisionlab.chat.decision.dto.DecisionDraftResponse.CriterionItem;
import com.decisionlab.chat.decision.dto.DecisionDraftResponse.FieldSources;
import com.decisionlab.chat.decision.dto.DecisionDraftResponse.Fields;
import com.decisionlab.chat.decision.entity.FieldSource;
import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.concern.entity.ConcernType;
import com.decisionlab.dilemma.entity.DilemmaType;
import com.decisionlab.session.dto.SessionDetailResponse.DecisionItem;
import com.decisionlab.session.dto.SessionDetailResponse.DilemmaItem;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.session.dto.SessionDetailResponse;
import com.decisionlab.session.dto.SessionDetailResponse.ConcernItem;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.repository.SessionRepository;
import com.decisionlab.session.service.SessionQueryService;
import com.decisionlab.session.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SessionQueryController.class)
class SessionQueryControllerTest {

    private static final String TOKEN = "token";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SessionQueryService sessionQueryService;

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
    void 명세서_4_2_형식으로_200이고_없는_값도_키는_null로_내려준다() throws Exception {
        when(sessionQueryService.getSession(sessionId)).thenReturn(new SessionDetailResponse(
                sessionId, SessionState.EXPLORING, PersonaCode.ACHIEVEMENT, 0,
                new ConcernItem("취업/대학원 고민", ConcernType.CAREER, List.of("취업", "대학원 진학"), null, null),
                null, null, null, null));

        mockMvc.perform(get("/api/v1/sessions/{id}", sessionId).header("X-Session-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeCode").isEmpty())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.state").value("EXPLORING"))
                .andExpect(jsonPath("$.currentPersonaCode").value("ACHIEVEMENT"))
                .andExpect(jsonPath("$.reconsiderCount").value(0))
                .andExpect(jsonPath("$.concern.freeText").value("취업/대학원 고민"))
                .andExpect(jsonPath("$.concern.concernType").value("CAREER"))
                .andExpect(jsonPath("$.concern.options[1]").value("대학원 진학"))
                .andExpect(jsonPath("$.concern.initialPreference").isEmpty())
                .andExpect(jsonPath("$.provisionalDecision").isEmpty())
                .andExpect(jsonPath("$.dilemma").isEmpty())
                .andExpect(jsonPath("$.reportId").isEmpty());
    }

    @Test
    void RECONSIDER면_잠정_결정과_딜레마_S08_보기를_JSON으로_내려준다() throws Exception {
        DecisionItem decision = new DecisionItem("12", 1, "요약",
                new Fields("대학원 진학", "연구 전문성", "unknown", "취업 시점의 지연", "실제 대학원 연구 경험"),
                new FieldSources(FieldSource.USER_STATED, FieldSource.AI_INFERRED, null,
                        FieldSource.AI_INFERRED, FieldSource.NEEDS_CHECK),
                List.of(new CriterionItem("7", "VALUE", "연구 전문성", CriterionSource.USER_STATED)));
        DilemmaItem dilemma = new DilemmaItem("5", DilemmaType.ALT_PATH, "학위 없이도 전문성을 쌓을 수 있다면?",
                "[가상 상황] ...", "검증 목표", false,
                List.of(new MessageOption("o1", "좋은 기업에서 일할 기회를 포기하는 게 아까워졌어요")));
        when(sessionQueryService.getSession(sessionId)).thenReturn(new SessionDetailResponse(
                sessionId, SessionState.RECONSIDER, PersonaCode.ACHIEVEMENT, 1, null, decision, dilemma, null, null));

        mockMvc.perform(get("/api/v1/sessions/{id}", sessionId).header("X-Session-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provisionalDecision.decisionId").value("12"))
                .andExpect(jsonPath("$.provisionalDecision.fields.decisionText").value("대학원 진학"))
                .andExpect(jsonPath("$.provisionalDecision.fieldSources.reason").isEmpty())
                .andExpect(jsonPath("$.provisionalDecision.fieldSources.uncheckedCondition").value("NEEDS_CHECK"))
                .andExpect(jsonPath("$.provisionalDecision.criteria[0].criterionId").value("7"))
                .andExpect(jsonPath("$.dilemma.dilemmaId").value("5"))
                .andExpect(jsonPath("$.dilemma.title").value("학위 없이도 전문성을 쌓을 수 있다면?"))
                .andExpect(jsonPath("$.dilemma.isFallback").value(false))
                .andExpect(jsonPath("$.dilemma.shakenValueOptions[0].optionId").value("o1"))
                .andExpect(jsonPath("$.dilemma.shakenValueOptions[0].label")
                        .value("좋은 기업에서 일할 기회를 포기하는 게 아까워졌어요"));
    }

    @Test
    void PAUSED면_S10_새로고침용_resumeCode를_내려준다() throws Exception {
        when(sessionQueryService.getSession(sessionId)).thenReturn(new SessionDetailResponse(
                sessionId, SessionState.PAUSED, PersonaCode.ACHIEVEMENT, 0, null, null, null, null, "ABCD-1234"));

        mockMvc.perform(get("/api/v1/sessions/{id}", sessionId).header("X-Session-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PAUSED"))
                .andExpect(jsonPath("$.resumeCode").value("ABCD-1234"));
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/{id}", sessionId))
                .andExpect(status().isUnauthorized());
        verify(sessionQueryService, never()).getSession(any());
    }

    @Test
    void 다른_세션이면_403() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/{id}", UUID.randomUUID()).header("X-Session-Token", TOKEN))
                .andExpect(status().isForbidden());
        verify(sessionQueryService, never()).getSession(any());
    }

    @Test
    void sessionId가_UUID_형식이_아니면_400() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/{id}", "not-a-uuid").header("X-Session-Token", TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
