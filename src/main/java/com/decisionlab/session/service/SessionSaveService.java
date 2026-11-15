package com.decisionlab.session.service;

import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * #13 그만 대화하기의 SAVE 모드 (EXPLORING → PAUSED + 이어하기 코드 발급, 명세서 4.13).
 * SessionEndService의 SAVE 분기에서 호출한다.
 */
@Service
public class SessionSaveService {

    // 26^4 * 10^4 가지라 몇 번 안에 겹치지 않는 코드가 나온다. 무한 반복을 막기 위한 상한
    private static final int MAX_CODE_ATTEMPTS = 10;

    private final SessionStateService sessionStateService;
    private final SessionRepository sessionRepository;
    private final ResumeCodeGenerator resumeCodeGenerator;

    public SessionSaveService(SessionStateService sessionStateService,
                              SessionRepository sessionRepository,
                              ResumeCodeGenerator resumeCodeGenerator) {
        this.sessionStateService = sessionStateService;
        this.sessionRepository = sessionRepository;
        this.resumeCodeGenerator = resumeCodeGenerator;
    }

    /** @return 새로 발급한 이어하기 코드 */
    @Transactional
    public String save(UUID sessionId) {
        sessionStateService.requireState(sessionId, SessionState.EXPLORING);

        // 상태 전이(조건부 업데이트)를 먼저 해서, 동시에 두 번 저장하면 늦은 쪽은 여기서 409로 끝나고 코드를 바꾸지 않는다.
        sessionStateService.transition(sessionId, SessionState.PAUSED);

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "세션을 찾을 수 없습니다."));
        String code = newUniqueCode();
        session.assignResumeCode(code);
        return code;
    }

    private String newUniqueCode() {
        for (int i = 0; i < MAX_CODE_ATTEMPTS; i++) {
            String code = resumeCodeGenerator.generate();
            if (!sessionRepository.existsByResumeCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("이어하기 코드를 발급하지 못했습니다. 다시 시도해 주세요.");
    }
}
