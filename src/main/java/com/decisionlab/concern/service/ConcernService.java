package com.decisionlab.concern.service;

import com.decisionlab.concern.dto.ConcernRequest;
import com.decisionlab.concern.dto.ConcernResponse;
import com.decisionlab.concern.entity.Concern;
import com.decisionlab.concern.repository.ConcernRepository;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.service.SessionStateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class ConcernService {

    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 3;

    private final ConcernRepository concernRepository;
    private final SessionStateService sessionStateService;

    public ConcernService(ConcernRepository concernRepository, SessionStateService sessionStateService) {
        this.concernRepository = concernRepository;
        this.sessionStateService = sessionStateService;
    }

    /**
     * #3 고민 입력. CREATED 상태에서만 가능하고, 이미 있으면 덮어쓴다. (CREATED → CREATED, 상태 변화 없음)
     */
    @Transactional
    public ConcernResponse saveConcern(UUID sessionId, ConcernRequest request) {
        sessionStateService.requireState(sessionId, SessionState.CREATED);

        List<String> options = normalizeOptions(request.options());
        String initialPreference = blankToNull(request.initialPreference());
        String reason = blankToNull(request.initialPreferenceReason());
        validatePreference(options, initialPreference);

        Concern concern = concernRepository.findBySessionId(sessionId)
                .map(existing -> {
                    existing.update(request.freeText(), request.concernType(), options, initialPreference, reason);
                    return existing;
                })
                .orElseGet(() -> concernRepository.save(Concern.builder()
                        .sessionId(sessionId)
                        .freeText(request.freeText())
                        .concernType(request.concernType())
                        .options(options)
                        .initialPreference(initialPreference)
                        .initialPreferenceReason(reason)
                        .build()));

        return ConcernResponse.of(SessionState.CREATED, concern.getId());
    }

    // 선택지는 선택사항이지만, 입력한다면 2~3개이고 서로 달라야 한다. 빈 목록은 "입력 안 함"으로 본다.
    private List<String> normalizeOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        List<String> trimmed = options.stream().map(String::strip).toList();
        if (trimmed.size() < MIN_OPTIONS || trimmed.size() > MAX_OPTIONS) {
            throw validation("선택지는 2~3개로 입력해 주세요.");
        }
        if (new HashSet<>(trimmed).size() != trimmed.size()) {
            throw validation("같은 선택지를 두 번 입력할 수 없습니다.");
        }
        return trimmed;
    }

    // 초기 선호는 입력한 선택지 중 하나여야 한다. 고르지 않았으면("아직 모르겠어요") null.
    private void validatePreference(List<String> options, String initialPreference) {
        if (initialPreference == null) {
            return;
        }
        if (options == null || !options.contains(initialPreference)) {
            throw validation("초기 선호는 입력한 선택지 중 하나여야 합니다.");
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.strip();
    }

    private ApiException validation(String message) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, message);
    }
}
