package com.decisionlab.chat.decision.service;

import com.decisionlab.chat.decision.dto.EditFieldRequest;
import com.decisionlab.chat.decision.dto.EditFieldResponse;
import com.decisionlab.chat.decision.entity.ProvisionalDecision;
import com.decisionlab.chat.decision.repository.ProvisionalDecisionRepository;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.service.SessionStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DecisionService {

    private static final Set<String> EDITABLE_FIELDS = Set.of(
            "decisionText", "coreValue", "reason", "acceptableCost", "uncheckedCondition");

    private final SessionStateService sessionStateService;
    private final ProvisionalDecisionRepository provisionalDecisionRepository;

    /** #9 잠정 결정 필드 수정 (S05, P1) */
    @Transactional
    public EditFieldResponse editField(UUID sessionId, Long decisionId, EditFieldRequest request) {
        sessionStateService.requireState(sessionId, SessionState.PROVISIONAL_DECISION);

        if (!EDITABLE_FIELDS.contains(request.field())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "field는 decisionText, coreValue, reason, acceptableCost, uncheckedCondition 중 하나여야 합니다.");
        }

        ProvisionalDecision decision = getOwnedDecision(sessionId, decisionId);
        decision.updateField(request.field(), request.value());

        return EditFieldResponse.from(decision);
    }

    private ProvisionalDecision getOwnedDecision(UUID sessionId, Long decisionId) {
        ProvisionalDecision decision = provisionalDecisionRepository.findById(decisionId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "잠정 결정을 찾을 수 없습니다."));
        if (!decision.getSessionId().equals(sessionId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "잠정 결정을 찾을 수 없습니다.");
        }
        return decision;
    }
}
