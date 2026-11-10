package com.decisionlab.session.service;

import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.session.dto.ResumeRequest;
import com.decisionlab.session.dto.ResumeResponse;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * #17 이어하기 (S01, S10). 토큰 없이 호출한다 (SessionTokenFilter 예외 경로).
 * 규칙: PAUSED → 바로 EXPLORING / 새 토큰 발급(이전 토큰은 끊김) / 코드는 다음 SAVE 때 교체되므로 여기서 지우지 않는다.
 */
@Service
public class SessionResumeService {

    private final SessionRepository sessionRepository;
    private final SessionStateService sessionStateService;
    private final SessionTokenGenerator tokenGenerator;
    private final Duration sessionTtl;

    public SessionResumeService(SessionRepository sessionRepository,
                                SessionStateService sessionStateService,
                                SessionTokenGenerator tokenGenerator,
                                @Value("${decisionlab.session.ttl:7d}") Duration sessionTtl) {
        this.sessionRepository = sessionRepository;
        this.sessionStateService = sessionStateService;
        this.tokenGenerator = tokenGenerator;
        this.sessionTtl = sessionTtl;
    }

    @Transactional
    public ResumeResponse resume(ResumeRequest request) {
        String code = normalize(request.resumeCode());

        Session session = sessionRepository.findByResumeCode(code)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "이어하기 코드를 찾을 수 없습니다."));
        if (session.isExpired()) {
            throw new ApiException(ErrorCode.SESSION_EXPIRED, "세션이 만료되었습니다.", session.getState().name());
        }

        // 이미 이어간 코드(상태가 PAUSED가 아님)면 409 + currentState.
        // 상태 전이를 먼저 해서, 같은 코드로 동시에 이어가면 늦은 쪽은 여기서 409로 끝나고 토큰을 바꾸지 않는다.
        sessionStateService.requireState(session.getId(), SessionState.PAUSED);
        sessionStateService.transition(session.getId(), SessionState.EXPLORING);

        String newToken = tokenGenerator.generate();
        session.rotateToken(newToken, LocalDateTime.now().plus(sessionTtl));

        return new ResumeResponse(session.getId(), newToken, SessionState.EXPLORING);
    }

    // 소문자, 앞뒤 공백, 하이픈 생략("abcd1234")까지 받아 준다.
    private String normalize(String raw) {
        String code = raw.strip().toUpperCase(Locale.ROOT);
        if (code.matches("^[A-Z]{4}[0-9]{4}$")) {
            code = code.substring(0, 4) + "-" + code.substring(4);
        }
        if (!code.matches(ResumeCodeGenerator.FORMAT)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "이어하기 코드는 ABCD-1234 형식입니다.");
        }
        return code;
    }
}
