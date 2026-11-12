package com.decisionlab.dilemma.service;

import com.decisionlab.ai.report.ReportGenerator;
import com.decisionlab.dilemma.dto.DilemmaFollowupRequest;
import com.decisionlab.dilemma.dto.DilemmaFollowupResponse;
import com.decisionlab.dilemma.dto.DilemmaRespondRequest;
import com.decisionlab.dilemma.dto.DilemmaRespondResponse;
import com.decisionlab.dilemma.entity.Dilemma;
import com.decisionlab.dilemma.entity.DilemmaChoice;
import com.decisionlab.dilemma.entity.DilemmaResponse;
import com.decisionlab.dilemma.entity.DilemmaType;
import com.decisionlab.dilemma.repository.DilemmaRepository;
import com.decisionlab.dilemma.repository.DilemmaResponseRepository;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.persona.entity.PersonaSwitch;
import com.decisionlab.persona.entity.SwitchReason;
import com.decisionlab.persona.repository.PersonaSwitchRepository;
import com.decisionlab.report.dto.ReportResult;
import com.decisionlab.report.entity.EntryPoint;
import com.decisionlab.report.entity.ReportType;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.service.SessionStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DilemmaServiceTest {

    @Mock
    DilemmaRepository dilemmaRepository;

    @Mock
    DilemmaResponseRepository dilemmaResponseRepository;

    @Mock
    PersonaSwitchRepository personaSwitchRepository;

    @Mock
    SessionStateService sessionStateService;

    @Mock
    ReportGenerator reportGenerator;

    @InjectMocks
    DilemmaService dilemmaService;

    private final UUID sessionId = UUID.randomUUID();
    private Dilemma dilemma;

    @BeforeEach
    void setUp() throws Exception {
        dilemma = Dilemma.builder()
                .sessionId(sessionId)
                .decisionId(1L)
                .dilemmaType(DilemmaType.ALT_PATH)
                .scenarioText("시나리오")
                .verificationGoal("검증 목표")
                .fallback(false)
                .build();
        Field idField = Dilemma.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(dilemma, 100L);
    }

    @Test
    void KEEP_선택_시_응답을_저장하고_REFLECTION으로_전이하며_질문과_선택지를_반환한다() {
        given(dilemmaRepository.findBySessionId(sessionId)).willReturn(Optional.of(dilemma));
        given(dilemmaResponseRepository.findByDilemmaId(100L)).willReturn(Optional.empty());

        DilemmaRespondResponse response = dilemmaService.respond(
                sessionId, new DilemmaRespondRequest(DilemmaChoice.KEEP));

        verify(sessionStateService).requireState(sessionId, SessionState.DILEMMA);
        ArgumentCaptor<DilemmaResponse> captor = ArgumentCaptor.forClass(DilemmaResponse.class);
        verify(dilemmaResponseRepository).save(captor.capture());
        assertThat(captor.getValue().getDilemmaId()).isEqualTo(100L);
        assertThat(captor.getValue().getChoice()).isEqualTo(DilemmaChoice.KEEP);

        verify(sessionStateService).transition(sessionId, SessionState.REFLECTION);
        assertThat(response.state()).isEqualTo(SessionState.REFLECTION);
        assertThat(response.prompt()).isNotBlank();
        assertThat(response.options()).isNotEmpty();
        assertThat(response.suggestedPersonaCode()).isNull();
        assertThat(response.reportId()).isNull();
    }

    @Test
    void RECONSIDER_선택_시_응답을_저장하고_RECONSIDER로_전이하며_선택지와_추천_자아를_반환한다() {
        given(dilemmaRepository.findBySessionId(sessionId)).willReturn(Optional.of(dilemma));
        given(dilemmaResponseRepository.findByDilemmaId(100L)).willReturn(Optional.empty());
        given(personaSwitchRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).willReturn(List.of(
                PersonaSwitch.builder()
                        .sessionId(sessionId)
                        .toPersonaCode(PersonaCode.ACHIEVEMENT)
                        .reason(SwitchReason.USER_SELECT)
                        .build()
        ));

        DilemmaRespondResponse response = dilemmaService.respond(
                sessionId, new DilemmaRespondRequest(DilemmaChoice.RECONSIDER));

        verify(sessionStateService).requireState(sessionId, SessionState.DILEMMA, SessionState.REFLECTION);
        verify(sessionStateService).transition(sessionId, SessionState.RECONSIDER);
        assertThat(response.state()).isEqualTo(SessionState.RECONSIDER);
        assertThat(response.prompt()).isNotBlank();
        assertThat(response.options()).isNotEmpty();
        assertThat(response.suggestedPersonaCode()).isEqualTo(PersonaCode.STABILITY);
        assertThat(response.reportId()).isNull();
    }

    @Test
    void 이미_RECONSIDER한_세션에서_다시_RECONSIDER_요청_시_409_RECONSIDER_LIMIT을_던진다() {
        given(dilemmaRepository.findBySessionId(sessionId)).willReturn(Optional.of(dilemma));
        DilemmaResponse existing = DilemmaResponse.builder()
                .dilemmaId(100L)
                .choice(DilemmaChoice.RECONSIDER)
                .build();
        given(dilemmaResponseRepository.findByDilemmaId(100L)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> dilemmaService.respond(
                sessionId, new DilemmaRespondRequest(DilemmaChoice.RECONSIDER)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECONSIDER_LIMIT);

        verify(sessionStateService, never()).transition(any(), any());
    }

    @Test
    void UNDECIDED_선택_시_UNDECIDED_REPORT를_생성하고_REPORT로_전이한다() {
        given(dilemmaRepository.findBySessionId(sessionId)).willReturn(Optional.of(dilemma));
        given(dilemmaResponseRepository.findByDilemmaId(100L)).willReturn(Optional.empty());
        given(reportGenerator.generate(sessionId, ReportType.UNDECIDED_REPORT, EntryPoint.S06))
                .willReturn(new ReportResult(
                        7L, ReportType.UNDECIDED_REPORT, EntryPoint.S06,
                        "요약", null, null, "가치", "상황", null, "미결정 이유", "부족 정보",
                        List.of(), "고지", LocalDateTime.now()
                ));

        DilemmaRespondResponse response = dilemmaService.respond(
                sessionId, new DilemmaRespondRequest(DilemmaChoice.UNDECIDED));

        verify(reportGenerator).generate(sessionId, ReportType.UNDECIDED_REPORT, EntryPoint.S06);
        verify(sessionStateService).transition(sessionId, SessionState.REPORT);
        assertThat(response.state()).isEqualTo(SessionState.REPORT);
        assertThat(response.reportId()).isEqualTo("7");
    }

    @Test
    void 딜레마가_없으면_404_NOT_FOUND_예외를_던진다() {
        given(dilemmaRepository.findBySessionId(sessionId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> dilemmaService.respond(
                sessionId, new DilemmaRespondRequest(DilemmaChoice.KEEP)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void keepReason_입력_시_유지_이유를_저장하고_리포트_생성_후_REPORT로_전이한다() {
        DilemmaResponse existing = DilemmaResponse.builder()
                .dilemmaId(100L)
                .choice(DilemmaChoice.KEEP)
                .build();
        given(dilemmaRepository.findBySessionId(sessionId)).willReturn(Optional.of(dilemma));
        given(dilemmaResponseRepository.findByDilemmaId(100L)).willReturn(Optional.of(existing));
        given(reportGenerator.generate(sessionId, ReportType.REPORT, EntryPoint.S07))
                .willReturn(new ReportResult(
                        15L, ReportType.REPORT, EntryPoint.S07,
                        "요약", "초기", "수정", "가치", "상황", "기준", null, "부족 정보",
                        List.of(), "고지", LocalDateTime.now()
                ));

        DilemmaFollowupResponse response = dilemmaService.followup(
                sessionId, new DilemmaFollowupRequest("체계적으로 연구 방법을 배우는 과정이 더 중요해요.", null));

        verify(sessionStateService).requireState(sessionId, SessionState.REFLECTION);
        assertThat(existing.getKeepReason()).isEqualTo("체계적으로 연구 방법을 배우는 과정이 더 중요해요.");
        verify(reportGenerator).generate(sessionId, ReportType.REPORT, EntryPoint.S07);
        verify(sessionStateService).transition(sessionId, SessionState.REPORT);
        assertThat(response.state()).isEqualTo(SessionState.REPORT);
        assertThat(response.reportId()).isEqualTo("15");
        assertThat(response.reconsiderCount()).isNull();
        assertThat(response.suggestedPersonaCode()).isNull();
    }

    @Test
    void shakenValue_입력_시_흔들린_가치를_저장하고_EXPLORING으로_전이하며_추천_자아를_반환한다() {
        DilemmaResponse existing = DilemmaResponse.builder()
                .dilemmaId(100L)
                .choice(DilemmaChoice.RECONSIDER)
                .build();
        given(dilemmaRepository.findBySessionId(sessionId)).willReturn(Optional.of(dilemma));
        given(dilemmaResponseRepository.findByDilemmaId(100L)).willReturn(Optional.of(existing));
        given(personaSwitchRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).willReturn(List.of(
                PersonaSwitch.builder()
                        .sessionId(sessionId)
                        .toPersonaCode(PersonaCode.ACHIEVEMENT)
                        .reason(SwitchReason.USER_SELECT)
                        .build()
        ));

        DilemmaFollowupResponse response = dilemmaService.followup(
                sessionId, new DilemmaFollowupRequest(null, "좋은 기업에서 일할 기회를 포기하는 게 아까워졌어요."));

        verify(sessionStateService).requireState(sessionId, SessionState.RECONSIDER);
        assertThat(existing.getShakenValue()).isEqualTo("좋은 기업에서 일할 기회를 포기하는 게 아까워졌어요.");
        verify(sessionStateService).transition(sessionId, SessionState.EXPLORING);
        assertThat(response.state()).isEqualTo(SessionState.EXPLORING);
        assertThat(response.reconsiderCount()).isEqualTo(1);
        assertThat(response.suggestedPersonaCode()).isEqualTo(PersonaCode.STABILITY);
        assertThat(response.reportId()).isNull();
    }

    @Test
    void keepReason과_shakenValue가_모두_없거나_둘_다_있으면_400_VALIDATION_ERROR를_던진다() {
        assertThatThrownBy(() -> dilemmaService.followup(
                sessionId, new DilemmaFollowupRequest(null, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> dilemmaService.followup(
                sessionId, new DilemmaFollowupRequest("유지 이유", "흔들린 가치")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        verify(sessionStateService, never()).transition(any(), any());
    }
}
