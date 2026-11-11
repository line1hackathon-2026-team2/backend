package com.decisionlab.dilemma.service;

import com.decisionlab.ai.report.ReportGenerator;
import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.dilemma.dto.DilemmaFollowupRequest;
import com.decisionlab.dilemma.dto.DilemmaFollowupResponse;
import com.decisionlab.dilemma.dto.DilemmaRespondRequest;
import com.decisionlab.dilemma.dto.DilemmaRespondResponse;
import com.decisionlab.dilemma.entity.Dilemma;
import com.decisionlab.dilemma.entity.DilemmaChoice;
import com.decisionlab.dilemma.entity.DilemmaResponse;
import com.decisionlab.dilemma.repository.DilemmaRepository;
import com.decisionlab.dilemma.repository.DilemmaResponseRepository;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.persona.entity.PersonaSwitch;
import com.decisionlab.persona.repository.PersonaSwitchRepository;
import com.decisionlab.report.dto.ReportResult;
import com.decisionlab.report.entity.EntryPoint;
import com.decisionlab.report.entity.ReportType;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.service.SessionStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DilemmaService {

    private static final String KEEP_PROMPT = "새로운 조건이 생겼는데도 같은 선택을 유지하는 이유가 무엇인가요?";
    private static final List<MessageOption> KEEP_OPTIONS = List.of(
            new MessageOption("o1", "처음 중요하게 생각한 가치가 여전히 가장 커요"),
            new MessageOption("o2", "제안된 대안보다 지금 선택의 장점이 더 확실해요"),
            new MessageOption("o3", "감수해야 할 부담을 고려해도 후회가 적을 것 같아요")
    );
    private static final String RECONSIDER_PROMPT = "어떤 부분이 마음을 흔들었나요?";
    private static final List<MessageOption> RECONSIDER_OPTIONS = List.of(
            new MessageOption("o1", "새로운 조건에서 제시된 기회를 포기하는 게 아까워졌어요"),
            new MessageOption("o2", "지금 선택이 아니어도 원하는 가치를 얻을 수 있을 것 같아요"),
            new MessageOption("o3", "미처 생각하지 못했던 현실적인 부담이 걱정되기 시작했어요")
    );

    private final DilemmaRepository dilemmaRepository;
    private final DilemmaResponseRepository dilemmaResponseRepository;
    private final PersonaSwitchRepository personaSwitchRepository;
    private final SessionStateService sessionStateService;
    private final ReportGenerator reportGenerator;

    /**
     * #11 딜레마 응답 (유지 / 흔들림 / 미결정 선택, 명세서 4.11).
     * KEEP -> REFLECTION, RECONSIDER -> RECONSIDER (REFLECTION에서 재흔들림 포함), UNDECIDED -> REPORT
     */
    @Transactional
    public DilemmaRespondResponse respond(UUID sessionId, DilemmaRespondRequest request) {
        DilemmaChoice choice = request.choice();
        if (choice == DilemmaChoice.RECONSIDER) {
            sessionStateService.requireState(sessionId, SessionState.DILEMMA, SessionState.REFLECTION);
        } else {
            sessionStateService.requireState(sessionId, SessionState.DILEMMA);
        }

        Dilemma dilemma = dilemmaRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "딜레마를 찾을 수 없습니다."));

        Optional<DilemmaResponse> existingOpt = dilemmaResponseRepository.findByDilemmaId(dilemma.getId());

        if (choice == DilemmaChoice.RECONSIDER && isAlreadyReconsidered(existingOpt)) {
            throw new ApiException(ErrorCode.RECONSIDER_LIMIT, "재검토는 세션당 1회만 가능합니다.");
        }

        saveOrUpdateChoice(dilemma.getId(), existingOpt, choice);

        return switch (choice) {
            case KEEP -> {
                sessionStateService.transition(sessionId, SessionState.REFLECTION);
                yield DilemmaRespondResponse.keep(KEEP_PROMPT, KEEP_OPTIONS);
            }
            case RECONSIDER -> {
                List<MessageOption> options = resolveReconsiderOptions(dilemma);
                PersonaCode suggestedPersona = suggestNextPersona(sessionId);
                sessionStateService.transition(sessionId, SessionState.RECONSIDER);
                yield DilemmaRespondResponse.reconsider(RECONSIDER_PROMPT, options, suggestedPersona);
            }
            case UNDECIDED -> {
                ReportResult reportResult = reportGenerator.generate(
                        sessionId, ReportType.UNDECIDED_REPORT, EntryPoint.S06);
                sessionStateService.transition(sessionId, SessionState.REPORT);
                yield DilemmaRespondResponse.undecided(reportResult.reportId());
            }
        };
    }

    /**
     * #12 딜레마 후속 입력 (유지 이유 / 흔들린 가치 입력, 명세서 4.12).
     * REFLECTION (keepReason) -> REPORT, RECONSIDER (shakenValue) -> EXPLORING
     */
    @Transactional
    public DilemmaFollowupResponse followup(UUID sessionId, DilemmaFollowupRequest request) {
        boolean hasKeepReason = StringUtils.hasText(request.keepReason());
        boolean hasShakenValue = StringUtils.hasText(request.shakenValue());

        if (hasKeepReason == hasShakenValue) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "keepReason 또는 shakenValue 중 하나만 입력해야 합니다.");
        }

        if (hasKeepReason) {
            sessionStateService.requireState(sessionId, SessionState.REFLECTION);
            DilemmaResponse dilemmaResponse = getDilemmaResponse(sessionId);
            dilemmaResponse.updateKeepReason(request.keepReason().trim());

            ReportResult reportResult = reportGenerator.generate(sessionId, ReportType.REPORT, EntryPoint.S07);
            sessionStateService.transition(sessionId, SessionState.REPORT);
            return DilemmaFollowupResponse.keep(reportResult.reportId());
        }

        sessionStateService.requireState(sessionId, SessionState.RECONSIDER);
        DilemmaResponse dilemmaResponse = getDilemmaResponse(sessionId);
        dilemmaResponse.updateShakenValue(request.shakenValue().trim());

        PersonaCode suggestedPersona = suggestNextPersona(sessionId);
        sessionStateService.transition(sessionId, SessionState.EXPLORING);
        return DilemmaFollowupResponse.reconsider(1, suggestedPersona);
    }

    private List<MessageOption> resolveReconsiderOptions(Dilemma dilemma) {
        List<MessageOption> stored = dilemma.getShakenValueOptions();
        if (stored != null && !stored.isEmpty()) {
            return stored;
        }
        dilemma.updateShakenValueOptions(RECONSIDER_OPTIONS);
        return RECONSIDER_OPTIONS;
    }

    private DilemmaResponse getDilemmaResponse(UUID sessionId) {
        Dilemma dilemma = dilemmaRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "딜레마를 찾을 수 없습니다."));
        return dilemmaResponseRepository.findByDilemmaId(dilemma.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "딜레마 응답을 찾을 수 없습니다."));
    }

    private boolean isAlreadyReconsidered(Optional<DilemmaResponse> existingOpt) {
        return existingOpt
                .map(resp -> resp.getChoice() == DilemmaChoice.RECONSIDER || resp.getShakenValue() != null)
                .orElse(false);
    }

    private void saveOrUpdateChoice(Long dilemmaId, Optional<DilemmaResponse> existingOpt, DilemmaChoice choice) {
        if (existingOpt.isPresent()) {
            existingOpt.get().updateChoice(choice);
            return;
        }
        dilemmaResponseRepository.save(DilemmaResponse.builder()
                .dilemmaId(dilemmaId)
                .choice(choice)
                .build());
    }

    private PersonaCode suggestNextPersona(UUID sessionId) {
        List<PersonaSwitch> switches = personaSwitchRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        if (switches.isEmpty()) {
            return PersonaCode.STABILITY;
        }
        PersonaCode currentPersona = switches.get(switches.size() - 1).getToPersonaCode();
        return switch (currentPersona) {
            case ACHIEVEMENT -> PersonaCode.STABILITY;
            case STABILITY -> PersonaCode.EXPLORATION;
            case EXPLORATION, DILEMMA_MANAGER -> PersonaCode.STABILITY;
        };
    }
}
