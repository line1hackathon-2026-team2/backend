package com.decisionlab.session.dto;

import com.decisionlab.chat.criterion.entity.JudgmentCriterion;
import com.decisionlab.chat.decision.dto.DecisionDraftResponse.CriterionItem;
import com.decisionlab.chat.decision.dto.DecisionDraftResponse.FieldSources;
import com.decisionlab.chat.decision.dto.DecisionDraftResponse.Fields;
import com.decisionlab.chat.decision.entity.ProvisionalDecision;
import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.concern.entity.Concern;
import com.decisionlab.concern.entity.ConcernType;
import com.decisionlab.dilemma.entity.Dilemma;
import com.decisionlab.dilemma.entity.DilemmaType;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.session.entity.SessionState;

import java.util.List;
import java.util.UUID;

/**
 * #2 세션 조회 응답 (명세서 4.2). 새로고침 시 프론트가 현재 화면을 복원하는 데 쓴다.
 * 아직 없는 값은 키를 유지한 채 null로 내려준다.
 *
 * @param currentPersonaCode 가장 최근 자아 선택/전환의 대상. 아직 자아를 고르지 않았으면 null
 * @param reconsiderCount    딜레마에서 RECONSIDER를 골랐으면 1, 아니면 0 (딜레마는 세션당 1개라 최대 1)
 * @param provisionalDecision 가장 최근 잠정 결정(리비전). 없으면 null. 형식은 4.8 응답에서 state만 뺀 것
 * @param dilemma            세션의 딜레마. 없으면 null
 * @param reportId           리포트가 있으면 숫자 문자열, 없으면 null
 * @param resumeCode         PAUSED일 때만 이어하기 코드 (S10 새로고침 복원용). 그 외에는 null
 */
public record SessionDetailResponse(
        UUID sessionId,
        SessionState state,
        PersonaCode currentPersonaCode,
        int reconsiderCount,
        ConcernItem concern,
        DecisionItem provisionalDecision,
        DilemmaItem dilemma,
        String reportId,
        String resumeCode
) {

    public record ConcernItem(
            String freeText,
            ConcernType concernType,
            List<String> options,
            String initialPreference,
            String initialPreferenceReason
    ) {
        public static ConcernItem from(Concern concern) {
            return new ConcernItem(
                    concern.getFreeText(),
                    concern.getConcernType(),
                    concern.getOptions(),
                    concern.getInitialPreference(),
                    concern.getInitialPreferenceReason()
            );
        }
    }

    /** 4.8 잠정 결정 응답과 같은 모양 (B의 DTO 조각을 그대로 써서 S05 화면을 같은 방식으로 그리게 한다) */
    public record DecisionItem(
            String decisionId,
            Integer revisionNo,
            String summaryText,
            Fields fields,
            FieldSources fieldSources,
            List<CriterionItem> criteria
    ) {
        public static DecisionItem from(ProvisionalDecision decision, List<JudgmentCriterion> criteria) {
            return new DecisionItem(
                    String.valueOf(decision.getId()),
                    decision.getRevisionNo(),
                    decision.getSummaryText(),
                    new Fields(
                            decision.getDecisionText(),
                            decision.getCoreValue(),
                            decision.getReason(),
                            decision.getAcceptableCost(),
                            decision.getUncheckedCondition()
                    ),
                    FieldSources.from(decision),
                    criteria.stream().map(CriterionItem::from).toList()
            );
        }
    }

    /**
     * 4.10 딜레마 객체와 같은 모양 + shakenValueOptions.
     *
     * @param shakenValueOptions RECONSIDER 상태일 때만 S08 보기를 저장된 그대로 내려준다(재생성 없음, 4.2/4.11). 그 외에는 null
     */
    public record DilemmaItem(
            String dilemmaId,
            DilemmaType dilemmaType,
            String title,
            String scenarioText,
            String verificationGoal,
            boolean isFallback,
            List<MessageOption> shakenValueOptions
    ) {
        public static DilemmaItem from(Dilemma dilemma, SessionState state) {
            return new DilemmaItem(
                    String.valueOf(dilemma.getId()),
                    dilemma.getDilemmaType(),
                    dilemma.getTitle(),
                    dilemma.getScenarioText(),
                    dilemma.getVerificationGoal(),
                    dilemma.isFallback(),
                    state == SessionState.RECONSIDER ? dilemma.getShakenValueOptions() : null
            );
        }
    }
}
