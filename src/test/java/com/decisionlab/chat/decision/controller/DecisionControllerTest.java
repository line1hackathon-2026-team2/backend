package com.decisionlab.chat.decision.controller;

import com.decisionlab.chat.criterion.entity.CriterionSource;
import com.decisionlab.chat.decision.dto.DecisionDraftResponse;
import com.decisionlab.chat.decision.dto.DecisionDraftResponse.CriterionItem;
import com.decisionlab.chat.decision.dto.DecisionDraftResponse.Fields;
import com.decisionlab.chat.decision.dto.DecisionDraftResponse.FieldSources;
import com.decisionlab.chat.decision.dto.EditFieldResponse;
import com.decisionlab.chat.decision.dto.ResolveResponse;
import com.decisionlab.chat.decision.entity.FieldSource;
import com.decisionlab.chat.decision.service.DecisionService;
import com.decisionlab.dilemma.dto.DilemmaResult;
import com.decisionlab.dilemma.entity.DilemmaType;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.entity.SessionState;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void 명세서_4_8_형식으로_잠정_결정_요약을_응답한다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(sessionRepository.findBySessionToken("token")).willReturn(Optional.of(session(sessionId)));
        DecisionDraftResponse response = new DecisionDraftResponse(
                SessionState.PROVISIONAL_DECISION,
                "10",
                1,
                "현재는 대학원 진학에 조금 더 마음이 기울어 있어요.",
                new Fields("대학원 진학", "연구 전문성", "연구개발 분야 희망", "취업 시점 지연", "실제 연구 경험"),
                new FieldSources(FieldSource.USER_STATED, FieldSource.AI_INFERRED, FieldSource.USER_STATED,
                        FieldSource.AI_INFERRED, FieldSource.NEEDS_CHECK),
                List.of(new CriterionItem("100", "VALUE", "연구 전문성", CriterionSource.USER_STATED))
        );
        given(decisionService.draft(any())).willReturn(response);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/decision/draft", sessionId)
                        .header("X-Session-Token", "token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PROVISIONAL_DECISION"))
                .andExpect(jsonPath("$.decisionId").value("10"))
                .andExpect(jsonPath("$.revisionNo").value(1))
                .andExpect(jsonPath("$.fields.decisionText").value("대학원 진학"))
                .andExpect(jsonPath("$.fieldSources.decisionText").value("USER_STATED"))
                .andExpect(jsonPath("$.fieldSources.coreValue").value("AI_INFERRED"))
                .andExpect(jsonPath("$.fieldSources.uncheckedCondition").value("NEEDS_CHECK"))
                .andExpect(jsonPath("$.criteria[0].criterionId").value("100"))
                .andExpect(jsonPath("$.criteria[0].source").value("USER_STATED"));
    }

    @Test
    void 승인_첫_시도는_명세서_4_10_딜레마_응답_형식이다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(sessionRepository.findBySessionToken("token")).willReturn(Optional.of(session(sessionId)));
        DilemmaResult dilemmaResult = new DilemmaResult(1L, DilemmaType.ALT_PATH, "제목", "시나리오", "검증 목표", false);
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
                .andExpect(jsonPath("$.dilemma.title").value("제목"))
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

    @Test
    void 필드_수정은_명세서_4_9_형식으로_응답한다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(sessionRepository.findBySessionToken("token")).willReturn(Optional.of(session(sessionId)));
        given(decisionService.editField(eq(sessionId), eq(10L), any())).willReturn(new EditFieldResponse(
                "10", 1,
                new EditFieldResponse.Fields("대학원 진학", "경제적 안정", "연구개발 분야 희망", "취업 시점 지연", "실제 연구 경험")
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
