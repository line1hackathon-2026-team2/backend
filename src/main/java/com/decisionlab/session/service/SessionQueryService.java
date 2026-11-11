package com.decisionlab.session.service;

import com.decisionlab.chat.criterion.repository.JudgmentCriterionRepository;
import com.decisionlab.chat.decision.repository.ProvisionalDecisionRepository;
import com.decisionlab.concern.repository.ConcernRepository;
import com.decisionlab.dilemma.entity.Dilemma;
import com.decisionlab.dilemma.entity.DilemmaChoice;
import com.decisionlab.dilemma.repository.DilemmaRepository;
import com.decisionlab.dilemma.repository.DilemmaResponseRepository;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.persona.entity.PersonaSwitch;
import com.decisionlab.persona.repository.PersonaSwitchRepository;
import com.decisionlab.report.repository.ReportRepository;
import com.decisionlab.session.dto.SessionDetailResponse;
import com.decisionlab.session.dto.SessionDetailResponse.ConcernItem;
import com.decisionlab.session.dto.SessionDetailResponse.DecisionItem;
import com.decisionlab.session.dto.SessionDetailResponse.DilemmaItem;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * #2 세션 조회. A의 테이블과 B, C의 테이블을 읽기만 해서 응답을 조합한다.
 * currentPersonaCode와 reconsiderCount는 별도 컬럼 없이 기존 데이터에서 계산한다.
 */
@Service
public class SessionQueryService {

    private final SessionRepository sessionRepository;
    private final ConcernRepository concernRepository;
    private final PersonaSwitchRepository personaSwitchRepository;
    private final ProvisionalDecisionRepository provisionalDecisionRepository;
    private final JudgmentCriterionRepository judgmentCriterionRepository;
    private final DilemmaRepository dilemmaRepository;
    private final DilemmaResponseRepository dilemmaResponseRepository;
    private final ReportRepository reportRepository;

    public SessionQueryService(SessionRepository sessionRepository,
                               ConcernRepository concernRepository,
                               PersonaSwitchRepository personaSwitchRepository,
                               ProvisionalDecisionRepository provisionalDecisionRepository,
                               JudgmentCriterionRepository judgmentCriterionRepository,
                               DilemmaRepository dilemmaRepository,
                               DilemmaResponseRepository dilemmaResponseRepository,
                               ReportRepository reportRepository) {
        this.sessionRepository = sessionRepository;
        this.concernRepository = concernRepository;
        this.personaSwitchRepository = personaSwitchRepository;
        this.provisionalDecisionRepository = provisionalDecisionRepository;
        this.judgmentCriterionRepository = judgmentCriterionRepository;
        this.dilemmaRepository = dilemmaRepository;
        this.dilemmaResponseRepository = dilemmaResponseRepository;
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public SessionDetailResponse getSession(UUID sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "세션을 찾을 수 없습니다."));
        SessionState state = session.getState();
        Optional<Dilemma> dilemma = dilemmaRepository.findBySessionId(sessionId);

        return new SessionDetailResponse(
                session.getId(),
                state,
                personaSwitchRepository.findFirstBySessionIdOrderByIdDesc(sessionId)
                        .map(PersonaSwitch::getToPersonaCode)
                        .orElse(null),
                reconsiderCount(dilemma),
                concernRepository.findBySessionId(sessionId).map(ConcernItem::from).orElse(null),
                latestDecision(sessionId),
                dilemma.map(d -> DilemmaItem.from(d, state)).orElse(null),
                reportRepository.findBySessionId(sessionId)
                        .map(report -> String.valueOf(report.getId()))
                        .orElse(null),
                // S10(저장 완료) 화면을 새로고침해도 코드를 다시 보여줄 수 있게, 저장된 상태일 때만 내려준다
                state == SessionState.PAUSED ? session.getResumeCode() : null
        );
    }

    // 리비전이 여러 개면 가장 최근 것. REVISE로 폐기된 결정은 B가 삭제하므로 남아 있지 않다.
    private DecisionItem latestDecision(UUID sessionId) {
        return provisionalDecisionRepository.findBySessionIdOrderByRevisionNoDesc(sessionId).stream()
                .findFirst()
                .map(decision -> DecisionItem.from(decision,
                        judgmentCriterionRepository.findByDecisionId(decision.getId())))
                .orElse(null);
    }

    // 딜레마 응답은 딜레마당 1행이고, S07에서 다시 RECONSIDER하면 같은 행의 choice가 바뀐다.
    private int reconsiderCount(Optional<Dilemma> dilemma) {
        boolean reconsidered = dilemma
                .flatMap(d -> dilemmaResponseRepository.findByDilemmaId(d.getId()))
                .map(response -> response.getChoice() == DilemmaChoice.RECONSIDER)
                .orElse(false);
        return reconsidered ? 1 : 0;
    }
}
