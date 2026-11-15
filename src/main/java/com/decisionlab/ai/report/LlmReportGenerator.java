package com.decisionlab.ai.report;

import com.decisionlab.ai.llm.LlmClient;
import com.decisionlab.chat.criterion.entity.JudgmentCriterion;
import com.decisionlab.chat.criterion.repository.JudgmentCriterionRepository;
import com.decisionlab.chat.decision.entity.ProvisionalDecision;
import com.decisionlab.chat.decision.repository.ProvisionalDecisionRepository;
import com.decisionlab.chat.message.entity.Message;
import com.decisionlab.chat.message.entity.MessageRole;
import com.decisionlab.chat.message.repository.MessageRepository;
import com.decisionlab.concern.entity.Concern;
import com.decisionlab.concern.repository.ConcernRepository;
import com.decisionlab.dilemma.entity.Dilemma;
import com.decisionlab.dilemma.entity.DilemmaResponse;
import com.decisionlab.dilemma.repository.DilemmaRepository;
import com.decisionlab.dilemma.repository.DilemmaResponseRepository;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.persona.entity.Persona;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.persona.repository.PersonaRepository;
import com.decisionlab.report.dto.ReportResult;
import com.decisionlab.report.entity.EntryPoint;
import com.decisionlab.report.entity.Report;
import com.decisionlab.report.entity.ReportAction;
import com.decisionlab.report.entity.ReportType;
import com.decisionlab.report.repository.ReportActionRepository;
import com.decisionlab.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LlmReportGenerator implements ReportGenerator {

    private static final String DEFAULT_DISCLAIMER = "이 리포트는 선택의 정답을 평가하지 않습니다.";
    private static final String UNKNOWN = "unknown";
    private static final int MAX_FIELD_LENGTH = 500;
    private static final int MAX_ACTION_LENGTH = 300;
    private static final int MAX_ACTIONS = 3;

    private final ReportRepository reportRepository;
    private final ReportActionRepository reportActionRepository;
    private final DilemmaRepository dilemmaRepository;
    private final DilemmaResponseRepository dilemmaResponseRepository;
    // A/B의 테이블은 읽기만 한다 (고민, 대화 이력, 자아 이름, 잠정 결정, 판단 기준 조회)
    private final ConcernRepository concernRepository;
    private final MessageRepository messageRepository;
    private final PersonaRepository personaRepository;
    private final ProvisionalDecisionRepository provisionalDecisionRepository;
    private final JudgmentCriterionRepository judgmentCriterionRepository;
    private final LlmClient llmClient;

    @Override
    @Transactional
    public ReportResult generate(UUID sessionId, ReportType type, EntryPoint entry) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("report_type", type.name());
        variables.put("entry_point", entry != null ? entry.name() : UNKNOWN);
        variables.put("concern", concernText(sessionId));
        variables.put("history", historyText(sessionId));
        variables.put("decisions", decisionsText(sessionId));
        variables.put("criteria", criteriaText(sessionId));
        variables.put("dilemma", dilemmaText(sessionId));

        GeneratedReport generated = llmClient.call("report-generator", variables, GeneratedReport.class);
        if (generated == null) {
            throw new ApiException(ErrorCode.LLM_FAILED, "리포트 생성 결과가 비어 있습니다.");
        }

        Report target = buildNormalizedReport(sessionId, type, entry, generated);

        Report report = reportRepository.findBySessionId(sessionId)
                .map(existing -> {
                    existing.update(
                            target.getReportType(),
                            target.getEntryPoint(),
                            target.getConcernSummary(),
                            target.getInitialView(),
                            target.getInitialViewReason(),
                            target.getRevisedView(),
                            target.getRevisedViewReason(),
                            target.getGuidingValues(),
                            target.getShakingSituation(),
                            target.getShakingSituationDetail(),
                            target.getClarifiedCriteria(),
                            target.getUndecidedReason(),
                            target.getMissingInfo(),
                            target.getDisclaimer()
                    );
                    Report updated = reportRepository.saveAndFlush(existing);
                    reportActionRepository.deleteByReportId(updated.getId());
                    return updated;
                })
                .orElseGet(() -> reportRepository.save(target));

        List<ReportAction> actions = saveNextActions(report.getId(), generated.nextActions());

        List<ReportResult.NextAction> nextActions = actions.stream()
                .map(action -> new ReportResult.NextAction(action.getSeq(), action.getContent()))
                .toList();

        LocalDateTime generatedAt = report.getUpdatedAt() != null
                ? report.getUpdatedAt()
                : (report.getCreatedAt() != null ? report.getCreatedAt() : LocalDateTime.now());

        return new ReportResult(
                report.getId(),
                report.getReportType(),
                report.getEntryPoint(),
                report.getConcernSummary(),
                report.getInitialView(),
                report.getInitialViewReason(),
                report.getRevisedView(),
                report.getRevisedViewReason(),
                report.getGuidingValues(),
                report.getShakingSituation(),
                report.getShakingSituationDetail(),
                report.getClarifiedCriteria(),
                report.getUndecidedReason(),
                report.getMissingInfo(),
                nextActions,
                report.getDisclaimer(),
                generatedAt
        );
    }

    private Report buildNormalizedReport(UUID sessionId, ReportType type, EntryPoint entry, GeneratedReport g) {
        String concernSummary = nonNullOrUnknown(g.concernSummary());
        String guidingValues = nonNullOrUnknown(g.guidingValues());
        String missingInfo = nonNullOrUnknown(g.missingInfo());

        return switch (type) {
            case REPORT -> Report.builder()
                    .sessionId(sessionId)
                    .reportType(ReportType.REPORT)
                    .entryPoint(entry)
                    .concernSummary(concernSummary)
                    .initialView(nonNullOrUnknown(g.initialView()))
                    .initialViewReason(nonNullOrUnknown(g.initialViewReason()))
                    .revisedView(nonNullOrUnknown(g.revisedView()))
                    .revisedViewReason(nonNullOrUnknown(g.revisedViewReason()))
                    .guidingValues(guidingValues)
                    .shakingSituation(nonNullOrUnknown(g.shakingSituation()))
                    .shakingSituationDetail(nonNullOrUnknown(g.shakingSituationDetail()))
                    .clarifiedCriteria(nonNullOrUnknown(g.clarifiedCriteria()))
                    .undecidedReason(null)
                    .missingInfo(missingInfo)
                    .disclaimer(DEFAULT_DISCLAIMER)
                    .build();

            case UNDECIDED_REPORT -> Report.builder()
                    .sessionId(sessionId)
                    .reportType(ReportType.UNDECIDED_REPORT)
                    .entryPoint(entry)
                    .concernSummary(concernSummary)
                    .initialView(null)
                    .initialViewReason(null)
                    .revisedView(null)
                    .revisedViewReason(null)
                    .guidingValues(guidingValues)
                    .shakingSituation(entry == EntryPoint.S06 ? nonNullOrUnknown(g.shakingSituation()) : null)
                    .shakingSituationDetail(entry == EntryPoint.S06 ? nonNullOrUnknown(g.shakingSituationDetail()) : null)
                    .clarifiedCriteria(null)
                    .undecidedReason(nonNullOrUnknown(g.undecidedReason()))
                    .missingInfo(missingInfo)
                    .disclaimer(DEFAULT_DISCLAIMER)
                    .build();

            case INCOMPLETE_REPORT -> Report.builder()
                    .sessionId(sessionId)
                    .reportType(ReportType.INCOMPLETE_REPORT)
                    .entryPoint(entry)
                    .concernSummary(concernSummary)
                    .initialView(null)
                    .initialViewReason(null)
                    .revisedView(null)
                    .revisedViewReason(null)
                    .guidingValues(guidingValues)
                    .shakingSituation(null)
                    .shakingSituationDetail(null)
                    .clarifiedCriteria(null)
                    .undecidedReason(null)
                    .missingInfo(missingInfo)
                    .disclaimer(DEFAULT_DISCLAIMER)
                    .build();
        };
    }

    private String nonNullOrUnknown(String value) {
        if (!StringUtils.hasText(value)) {
            return UNKNOWN;
        }
        String trimmed = value.trim();
        return trimmed.length() > MAX_FIELD_LENGTH ? trimmed.substring(0, MAX_FIELD_LENGTH) : trimmed;
    }

    private List<ReportAction> saveNextActions(Long reportId, List<String> rawActions) {
        if (rawActions == null || rawActions.isEmpty()) {
            return List.of();
        }
        List<String> validContents = rawActions.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(c -> c.length() > MAX_ACTION_LENGTH ? c.substring(0, MAX_ACTION_LENGTH) : c)
                .limit(MAX_ACTIONS)
                .toList();

        if (validContents.isEmpty()) {
            return List.of();
        }

        List<ReportAction> actions = new ArrayList<>(validContents.size());
        for (int i = 0; i < validContents.size(); i++) {
            actions.add(ReportAction.builder()
                    .reportId(reportId)
                    .seq(i + 1)
                    .content(validContents.get(i))
                    .build());
        }
        return reportActionRepository.saveAll(actions);
    }

    private String concernText(UUID sessionId) {
        return concernRepository.findBySessionId(sessionId)
                .map(Concern::toPromptText)
                .orElse("아직 입력되지 않음");
    }

    private String historyText(UUID sessionId) {
        List<Message> messages = messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId);
        if (messages.isEmpty()) {
            return "대화 내역 없음";
        }
        Map<PersonaCode, String> personaNames = personaRepository.findAll().stream()
                .collect(Collectors.toMap(Persona::getCode, Persona::getName));

        return messages.stream()
                .map(m -> (m.getRole() == MessageRole.AI
                        ? personaNames.getOrDefault(m.getPersonaCode(), m.getPersonaCode().name())
                        : "사용자") + ": " + m.getContent())
                .collect(Collectors.joining("\n"));
    }

    private String decisionsText(UUID sessionId) {
        List<ProvisionalDecision> decisions = new ArrayList<>(
                provisionalDecisionRepository.findBySessionIdOrderByRevisionNoDesc(sessionId));
        if (decisions.isEmpty()) {
            return "잠정 결정 기록 없음";
        }
        decisions.sort(Comparator.comparingInt(ProvisionalDecision::getRevisionNo));
        return decisions.stream()
                .map(d -> "[리비전 " + d.getRevisionNo() + "] "
                        + "선택=" + d.getDecisionText()
                        + ", 핵심가치=" + d.getCoreValue()
                        + ", 이유=" + d.getReason()
                        + ", 감수비용=" + d.getAcceptableCost()
                        + ", 미확인조건=" + d.getUncheckedCondition()
                        + ", 처리결과=" + d.getResolvedAction()
                        + ", 요약=" + d.getSummaryText())
                .collect(Collectors.joining("\n"));
    }

    private String criteriaText(UUID sessionId) {
        List<ProvisionalDecision> decisions = provisionalDecisionRepository
                .findBySessionIdOrderByRevisionNoDesc(sessionId);
        if (decisions.isEmpty()) {
            return "추출된 판단 기준 없음";
        }
        Long latestDecisionId = decisions.get(0).getId();
        List<JudgmentCriterion> criteria = judgmentCriterionRepository.findByDecisionId(latestDecisionId);
        if (criteria.isEmpty()) {
            return "추출된 판단 기준 없음";
        }
        return criteria.stream()
                .map(c -> "- [" + c.getCategory() + "] " + c.getContent() + " (" + c.getSource() + ")")
                .collect(Collectors.joining("\n"));
    }

    private String dilemmaText(UUID sessionId) {
        Optional<Dilemma> dilemmaOpt = dilemmaRepository.findBySessionId(sessionId);
        if (dilemmaOpt.isEmpty()) {
            return "딜레마 진행 전";
        }
        Dilemma d = dilemmaOpt.get();
        String base = "- 딜레마 유형: " + d.getDilemmaType() + "\n"
                + "- 딜레마 제목: " + d.getTitle() + "\n"
                + "- 가상 상황: " + d.getScenarioText() + "\n"
                + "- 검증 목표: " + d.getVerificationGoal();

        Optional<DilemmaResponse> respOpt = dilemmaResponseRepository.findByDilemmaId(d.getId());
        if (respOpt.isEmpty()) {
            return base + "\n- 사용자 응답: 없음";
        }
        DilemmaResponse r = respOpt.get();
        return base + "\n- 사용자 선택(choice): " + r.getChoice()
                + "\n- 유지 이유(keepReason): " + (r.getKeepReason() != null ? r.getKeepReason() : "없음")
                + "\n- 흔들린 가치(shakenValue): " + (r.getShakenValue() != null ? r.getShakenValue() : "없음");
    }

    /** LLM 구조화 출력 전용 DTO */
    public record GeneratedReport(
            String concernSummary,
            String initialView,
            String initialViewReason,
            String revisedView,
            String revisedViewReason,
            String guidingValues,
            String shakingSituation,
            String shakingSituationDetail,
            String clarifiedCriteria,
            String undecidedReason,
            String missingInfo,
            List<String> nextActions
    ) {
    }
}
