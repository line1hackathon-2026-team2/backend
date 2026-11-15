package com.decisionlab.ai.dilemma;

import com.decisionlab.ai.llm.LlmClient;
import com.decisionlab.chat.criterion.entity.JudgmentCriterion;
import com.decisionlab.chat.criterion.repository.JudgmentCriterionRepository;
import com.decisionlab.chat.decision.entity.ProvisionalDecision;
import com.decisionlab.chat.decision.repository.ProvisionalDecisionRepository;
import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.concern.entity.Concern;
import com.decisionlab.concern.repository.ConcernRepository;
import com.decisionlab.dilemma.dto.DilemmaResult;
import com.decisionlab.dilemma.entity.Dilemma;
import com.decisionlab.dilemma.entity.DilemmaTemplate;
import com.decisionlab.dilemma.entity.DilemmaType;
import com.decisionlab.dilemma.repository.DilemmaRepository;
import com.decisionlab.dilemma.repository.DilemmaTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmDilemmaGenerator implements DilemmaGenerator {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_SCENARIO_LENGTH = 1000;
    private static final int MAX_GOAL_LENGTH = 300;
    private static final int MAX_OPTION_LABEL_LENGTH = 50;
    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 3;

    private static final List<MessageOption> DEFAULT_SHAKEN_OPTIONS = List.of(
            new MessageOption("o1", "새로운 조건에서 제시된 기회를 포기하는 게 아까워졌어요"),
            new MessageOption("o2", "지금 선택이 아니어도 원하는 가치를 얻을 수 있을 것 같아요")
    );

    private final DilemmaRepository dilemmaRepository;
    private final DilemmaTemplateRepository dilemmaTemplateRepository;
    // A/B의 테이블은 읽기만 한다 (고민, 잠정 결정, 판단 기준 조회)
    private final ConcernRepository concernRepository;
    private final ProvisionalDecisionRepository provisionalDecisionRepository;
    private final JudgmentCriterionRepository judgmentCriterionRepository;
    private final LlmClient llmClient;

    @Override
    @Transactional
    public DilemmaResult generate(UUID sessionId, Long decisionId) {
        Optional<Dilemma> existing = dilemmaRepository.findBySessionId(sessionId);
        if (existing.isPresent()) {
            return toResult(existing.get());
        }

        Dilemma dilemma = generateOrFallback(sessionId, decisionId);
        Dilemma saved = dilemmaRepository.save(dilemma);
        return toResult(saved);
    }

    private Dilemma generateOrFallback(UUID sessionId, Long decisionId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("concern", concernText(sessionId));
        variables.put("decision", decisionText(decisionId));
        variables.put("criteria", criteriaText(decisionId));

        try {
            GeneratedDilemma generated = llmClient.call("dilemma-generator", variables, GeneratedDilemma.class);
            if (isValid(generated)) {
                List<MessageOption> options = resolveShakenValueOptions(
                        generated.dilemmaType(), generated.shakenValueOptions());
                return Dilemma.builder()
                        .sessionId(sessionId)
                        .decisionId(decisionId)
                        .dilemmaType(generated.dilemmaType())
                        .title(generated.title().trim())
                        .scenarioText(generated.scenarioText().trim())
                        .verificationGoal(generated.verificationGoal().trim())
                        .shakenValueOptions(options)
                        .fallback(false)
                        .build();
            }
            log.warn("생성된 딜레마 품질 조건 미충족 - 템플릿으로 폴백합니다 (sessionId={})", sessionId);
        } catch (RuntimeException e) {
            log.warn("딜레마 LLM 생성 실패 - 템플릿으로 폴백합니다 (sessionId={})", sessionId, e);
        }

        return buildFallbackDilemma(sessionId, decisionId);
    }

    private boolean isValid(GeneratedDilemma generated) {
        if (generated == null || generated.dilemmaType() == null) {
            return false;
        }
        return isValidText(generated.title(), MAX_TITLE_LENGTH)
                && isValidText(generated.scenarioText(), MAX_SCENARIO_LENGTH)
                && isValidText(generated.verificationGoal(), MAX_GOAL_LENGTH);
    }

    private boolean isValidText(String text, int maxLength) {
        return StringUtils.hasText(text) && text.trim().length() <= maxLength;
    }

    private List<MessageOption> resolveShakenValueOptions(DilemmaType dilemmaType, List<String> rawOptions) {
        if (rawOptions != null) {
            List<String> validLabels = rawOptions.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .map(label -> label.length() > MAX_OPTION_LABEL_LENGTH
                            ? label.substring(0, MAX_OPTION_LABEL_LENGTH)
                            : label)
                    .limit(MAX_OPTIONS)
                    .toList();

            if (validLabels.size() >= MIN_OPTIONS) {
                List<MessageOption> options = new ArrayList<>(validLabels.size());
                for (int i = 0; i < validLabels.size(); i++) {
                    options.add(new MessageOption("o" + (i + 1), validLabels.get(i)));
                }
                return options;
            }
        }

        return dilemmaTemplateRepository.findByDilemmaType(dilemmaType)
                .map(DilemmaTemplate::getShakenValueOptions)
                .filter(opts -> opts != null && !opts.isEmpty())
                .orElse(DEFAULT_SHAKEN_OPTIONS);
    }

    private Dilemma buildFallbackDilemma(UUID sessionId, Long decisionId) {
        Optional<DilemmaTemplate> templateOpt = dilemmaTemplateRepository.findByDilemmaType(DilemmaType.ALT_PATH)
                .or(() -> dilemmaTemplateRepository.findAll().stream().findFirst());

        if (templateOpt.isPresent()) {
            DilemmaTemplate template = templateOpt.get();
            List<MessageOption> options = (template.getShakenValueOptions() != null
                    && !template.getShakenValueOptions().isEmpty())
                    ? template.getShakenValueOptions()
                    : DEFAULT_SHAKEN_OPTIONS;
            return Dilemma.builder()
                    .sessionId(sessionId)
                    .decisionId(decisionId)
                    .dilemmaType(template.getDilemmaType())
                    .title(template.getTitle())
                    .scenarioText(template.getScenarioText())
                    .verificationGoal(template.getVerificationGoal())
                    .shakenValueOptions(options)
                    .fallback(true)
                    .build();
        }

        return Dilemma.builder()
                .sessionId(sessionId)
                .decisionId(decisionId)
                .dilemmaType(DilemmaType.ALT_PATH)
                .title("학위 없이도 전문성을 쌓을 수 있다면?")
                .scenarioText("[가상 상황] 희망하던 기업에서 전문 연구를 수행할 수 있는 연구개발 직무를 제안했습니다. "
                        + "학위 없이도 원하는 역량을 쌓을 수 있다면, 지금의 판단은 달라질까요?")
                .verificationGoal("전문성을 얻는 방법이 대학원이어야 하는가")
                .shakenValueOptions(DEFAULT_SHAKEN_OPTIONS)
                .fallback(true)
                .build();
    }

    private String concernText(UUID sessionId) {
        return concernRepository.findBySessionId(sessionId)
                .map(Concern::toPromptText)
                .orElse("아직 입력되지 않음");
    }

    private String decisionText(Long decisionId) {
        return provisionalDecisionRepository.findById(decisionId)
                .map(this::formatDecision)
                .orElse("잠정 결정 정보 없음");
    }

    private String formatDecision(ProvisionalDecision d) {
        return "- 요약: " + d.getSummaryText() + "\n"
                + "- 잠정 선택(decisionText): " + d.getDecisionText() + "\n"
                + "- 핵심 가치(coreValue): " + d.getCoreValue() + "\n"
                + "- 이유(reason): " + d.getReason() + "\n"
                + "- 감수할 비용(acceptableCost): " + d.getAcceptableCost() + "\n"
                + "- 미확인 조건(uncheckedCondition): " + d.getUncheckedCondition();
    }

    private String criteriaText(Long decisionId) {
        List<JudgmentCriterion> criteria = judgmentCriterionRepository.findByDecisionId(decisionId);
        if (criteria.isEmpty()) {
            return "추출된 판단 기준 없음";
        }
        return criteria.stream()
                .map(c -> "- [" + c.getCategory() + "] " + c.getContent() + " (" + c.getSource() + ")")
                .collect(Collectors.joining("\n"));
    }

    private DilemmaResult toResult(Dilemma dilemma) {
        return new DilemmaResult(
                dilemma.getId(),
                dilemma.getDilemmaType(),
                dilemma.getTitle(),
                dilemma.getScenarioText(),
                dilemma.getVerificationGoal(),
                dilemma.isFallback()
        );
    }

    /** LLM 구조화 출력 전용 DTO */
    public record GeneratedDilemma(
            DilemmaType dilemmaType,
            String title,
            String scenarioText,
            String verificationGoal,
            List<String> shakenValueOptions
    ) {
    }
}
