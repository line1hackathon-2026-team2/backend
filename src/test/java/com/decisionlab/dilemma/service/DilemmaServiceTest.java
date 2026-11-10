package com.decisionlab.dilemma.service;

import com.decisionlab.ai.report.ReportGenerator;
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
}
