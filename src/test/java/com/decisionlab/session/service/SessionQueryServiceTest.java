package com.decisionlab.session.service;

import com.decisionlab.chat.criterion.entity.CriterionSource;
import com.decisionlab.chat.criterion.entity.JudgmentCriterion;
import com.decisionlab.chat.criterion.repository.JudgmentCriterionRepository;
import com.decisionlab.chat.decision.entity.FieldSource;
import com.decisionlab.chat.decision.entity.ProvisionalDecision;
import com.decisionlab.chat.decision.repository.ProvisionalDecisionRepository;
import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.concern.entity.Concern;
import com.decisionlab.concern.entity.ConcernType;
import com.decisionlab.concern.repository.ConcernRepository;
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
import com.decisionlab.report.entity.Report;
import com.decisionlab.report.repository.ReportRepository;
import com.decisionlab.session.dto.SessionDetailResponse;
import com.decisionlab.session.dto.SessionDetailResponse.DecisionItem;
import com.decisionlab.session.dto.SessionDetailResponse.DilemmaItem;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionQueryServiceTest {

    private final SessionRepository sessionRepository = mock(SessionRepository.class);
    private final ConcernRepository concernRepository = mock(ConcernRepository.class);
    private final PersonaSwitchRepository personaSwitchRepository = mock(PersonaSwitchRepository.class);
    private final ProvisionalDecisionRepository provisionalDecisionRepository = mock(ProvisionalDecisionRepository.class);
    private final JudgmentCriterionRepository judgmentCriterionRepository = mock(JudgmentCriterionRepository.class);
    private final DilemmaRepository dilemmaRepository = mock(DilemmaRepository.class);
    private final DilemmaResponseRepository dilemmaResponseRepository = mock(DilemmaResponseRepository.class);
    private final ReportRepository reportRepository = mock(ReportRepository.class);

    private final SessionQueryService service = new SessionQueryService(sessionRepository, concernRepository,
            personaSwitchRepository, provisionalDecisionRepository, judgmentCriterionRepository,
            dilemmaRepository, dilemmaResponseRepository, reportRepository);

    private final UUID sessionId = UUID.randomUUID();
    private Session session;

    private static final List<MessageOption> SHAKEN_OPTIONS = List.of(
            new MessageOption("o1", "좋은 기업에서 일할 기회를 포기하는 게 아까워졌어요"),
            new MessageOption("o2", "대학원이 아니어도 연구 역량을 쌓을 수 있을 것 같아요"));

    @BeforeEach
    void setUp() {
        session = new Session("token", LocalDateTime.now().plusDays(7));
        ReflectionTestUtils.setField(session, "id", sessionId);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
    }

    private Dilemma givenDilemma() {
        Dilemma dilemma = Dilemma.builder()
                .sessionId(sessionId).decisionId(3L).dilemmaType(DilemmaType.ALT_PATH)
                .title("학위 없이도 전문성을 쌓을 수 있다면?").scenarioText("[가상 상황] ...")
                .verificationGoal("전문성을 얻는 방법이 대학원이어야 하는가")
                .shakenValueOptions(SHAKEN_OPTIONS).fallback(false)
                .build();
        ReflectionTestUtils.setField(dilemma, "id", 5L);
        when(dilemmaRepository.findBySessionId(sessionId)).thenReturn(Optional.of(dilemma));
        return dilemma;
    }

    private void givenDilemmaResponse(DilemmaChoice choice) {
        givenDilemma();
        when(dilemmaResponseRepository.findByDilemmaId(5L))
                .thenReturn(Optional.of(DilemmaResponse.builder().dilemmaId(5L).choice(choice).build()));
    }

    private ProvisionalDecision decision(long id, int revisionNo, String decisionText) {
        ProvisionalDecision decision = ProvisionalDecision.builder()
                .sessionId(sessionId).revisionNo(revisionNo).summaryText("요약 " + revisionNo)
                .decisionText(decisionText).decisionTextSource(FieldSource.USER_STATED)
                .coreValue("연구 전문성").coreValueSource(FieldSource.AI_INFERRED)
                .reason("unknown").reasonSource(null)
                .acceptableCost("취업 시점의 지연").acceptableCostSource(FieldSource.AI_INFERRED)
                .uncheckedCondition("실제 대학원 연구 경험")
                .build();
        ReflectionTestUtils.setField(decision, "id", id);
        return decision;
    }

    // ---- 1차: develop에 있던 데이터 ----

    @Test
    void 방금_만든_세션은_상태만_있고_나머지는_비어_있다() {
        SessionDetailResponse response = service.getSession(sessionId);

        assertThat(response.sessionId()).isEqualTo(sessionId);
        assertThat(response.state()).isEqualTo(SessionState.CREATED);
        assertThat(response.currentPersonaCode()).isNull();
        assertThat(response.reconsiderCount()).isZero();
        assertThat(response.concern()).isNull();
        assertThat(response.provisionalDecision()).isNull();
        assertThat(response.dilemma()).isNull();
        assertThat(response.reportId()).isNull();
        assertThat(response.resumeCode()).isNull();
    }

    @Test
    void 고민의_모든_필드를_돌려준다() {
        when(concernRepository.findBySessionId(sessionId)).thenReturn(Optional.of(Concern.builder()
                .sessionId(sessionId).freeText("취업/대학원 고민").concernType(ConcernType.CAREER)
                .options(List.of("취업", "대학원 진학")).initialPreference("취업").initialPreferenceReason("빨리 일하고 싶어")
                .build()));

        SessionDetailResponse.ConcernItem concern = service.getSession(sessionId).concern();

        assertThat(concern.freeText()).isEqualTo("취업/대학원 고민");
        assertThat(concern.concernType()).isEqualTo(ConcernType.CAREER);
        assertThat(concern.options()).containsExactly("취업", "대학원 진학");
        assertThat(concern.initialPreference()).isEqualTo("취업");
        assertThat(concern.initialPreferenceReason()).isEqualTo("빨리 일하고 싶어");
    }

    @Test
    void 현재_자아는_가장_최근_전환의_대상이다() {
        when(personaSwitchRepository.findFirstBySessionIdOrderByIdDesc(sessionId)).thenReturn(Optional.of(
                PersonaSwitch.builder().sessionId(sessionId).fromPersonaCode(PersonaCode.ACHIEVEMENT)
                        .toPersonaCode(PersonaCode.STABILITY).reason(SwitchReason.SWITCH).build()));

        assertThat(service.getSession(sessionId).currentPersonaCode()).isEqualTo(PersonaCode.STABILITY);
    }

    @Test
    void 딜레마에서_RECONSIDER를_골랐으면_reconsiderCount는_1() {
        givenDilemmaResponse(DilemmaChoice.RECONSIDER);

        assertThat(service.getSession(sessionId).reconsiderCount()).isEqualTo(1);
    }

    @Test
    void 딜레마에서_KEEP을_골랐으면_reconsiderCount는_0() {
        givenDilemmaResponse(DilemmaChoice.KEEP);

        assertThat(service.getSession(sessionId).reconsiderCount()).isZero();
    }

    @Test
    void 딜레마는_있지만_아직_응답하지_않았으면_reconsiderCount는_0() {
        givenDilemma();
        when(dilemmaResponseRepository.findByDilemmaId(5L)).thenReturn(Optional.empty());

        assertThat(service.getSession(sessionId).reconsiderCount()).isZero();
    }

    @Test
    void 리포트가_있으면_reportId를_문자열로_돌려준다() {
        Report report = Report.builder().sessionId(sessionId).build();
        ReflectionTestUtils.setField(report, "id", 15L);
        when(reportRepository.findBySessionId(sessionId)).thenReturn(Optional.of(report));

        assertThat(service.getSession(sessionId).reportId()).isEqualTo("15");
    }

    @Test
    void 세션이_없으면_404() {
        UUID unknown = UUID.randomUUID();
        when(sessionRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSession(unknown))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    // ---- 2차: 잠정 결정(B V3), 딜레마(C V4), 이어하기 코드(#17) ----

    @Test
    void 잠정_결정은_가장_최근_리비전을_필드_출처_기준과_함께_돌려준다() {
        session.transitionTo(SessionState.PROVISIONAL_DECISION);
        when(provisionalDecisionRepository.findBySessionIdOrderByRevisionNoDesc(sessionId))
                .thenReturn(List.of(decision(12L, 2, "대학원 진학 유지"), decision(11L, 1, "대학원 진학")));
        when(judgmentCriterionRepository.findByDecisionId(12L)).thenReturn(List.of(
                JudgmentCriterion.builder().sessionId(sessionId).decisionId(12L)
                        .category("VALUE").content("연구 전문성").source(CriterionSource.USER_STATED).build()));

        DecisionItem decision = service.getSession(sessionId).provisionalDecision();

        assertThat(decision.decisionId()).isEqualTo("12");
        assertThat(decision.revisionNo()).isEqualTo(2);
        assertThat(decision.summaryText()).isEqualTo("요약 2");
        assertThat(decision.fields().decisionText()).isEqualTo("대학원 진학 유지");
        assertThat(decision.fieldSources().decisionText()).isEqualTo(FieldSource.USER_STATED);
        assertThat(decision.fieldSources().coreValue()).isEqualTo(FieldSource.AI_INFERRED);
        // 값이 unknown이면 출처는 null, uncheckedCondition은 항상 NEEDS_CHECK (PR #20 결정)
        assertThat(decision.fields().reason()).isEqualTo("unknown");
        assertThat(decision.fieldSources().reason()).isNull();
        assertThat(decision.fieldSources().uncheckedCondition()).isEqualTo(FieldSource.NEEDS_CHECK);
        assertThat(decision.criteria()).singleElement()
                .satisfies(c -> assertThat(c.content()).isEqualTo("연구 전문성"));
    }

    @Test
    void 딜레마는_4_10_형식으로_돌려주고_RECONSIDER가_아니면_보기는_null() {
        session.transitionTo(SessionState.DILEMMA);
        givenDilemma();

        DilemmaItem dilemma = service.getSession(sessionId).dilemma();

        assertThat(dilemma.dilemmaId()).isEqualTo("5");
        assertThat(dilemma.dilemmaType()).isEqualTo(DilemmaType.ALT_PATH);
        assertThat(dilemma.title()).isEqualTo("학위 없이도 전문성을 쌓을 수 있다면?");
        assertThat(dilemma.verificationGoal()).isEqualTo("전문성을 얻는 방법이 대학원이어야 하는가");
        assertThat(dilemma.isFallback()).isFalse();
        assertThat(dilemma.shakenValueOptions()).isNull();
    }

    @Test
    void RECONSIDER_상태면_S08_보기를_저장된_그대로_복원한다() {
        session.transitionTo(SessionState.RECONSIDER);
        givenDilemmaResponse(DilemmaChoice.RECONSIDER);

        SessionDetailResponse response = service.getSession(sessionId);

        assertThat(response.dilemma().shakenValueOptions()).isEqualTo(SHAKEN_OPTIONS);
        assertThat(response.reconsiderCount()).isEqualTo(1);
    }

    @Test
    void PAUSED면_S10_새로고침용_이어하기_코드를_돌려준다() {
        session.transitionTo(SessionState.PAUSED);
        session.assignResumeCode("ABCD-1234");

        assertThat(service.getSession(sessionId).resumeCode()).isEqualTo("ABCD-1234");
    }

    @Test
    void 이어간_뒤에는_코드가_DB에_남아_있어도_내려주지_않는다() {
        session.transitionTo(SessionState.EXPLORING);
        session.assignResumeCode("ABCD-1234");

        assertThat(service.getSession(sessionId).resumeCode()).isNull();
    }
}
