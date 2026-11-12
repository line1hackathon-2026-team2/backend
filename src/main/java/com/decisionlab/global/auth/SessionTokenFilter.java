package com.decisionlab.global.auth;

import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.repository.SessionRepository;
import com.decisionlab.session.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SessionTokenFilter extends OncePerRequestFilter {

    private static final String SESSION_TOKEN_HEADER = "X-Session-Token";
    private static final Pattern SESSION_ID_IN_PATH = Pattern.compile("/api/v1/sessions/([^/]+)");
    private static final List<String> NO_TOKEN_PREFIXES = List.of("/swagger-ui", "/v3/api-docs");
    // #15 리포트 공개 조회: reportId만 알면 누구나 조회 (4.15). /sessions/{id}/report(#14)는 여전히 토큰 필요
    private static final Pattern PUBLIC_REPORT_PATH = Pattern.compile("/api/v1/reports/[^/]+");

    private final SessionRepository sessionRepository;
    private final SessionService sessionService;
    // 필터 예외는 @RestControllerAdvice까지 가지 않으므로, 리졸버에 넘겨 GlobalExceptionHandler가 처리하게 한다.
    private final HandlerExceptionResolver exceptionResolver;

    public SessionTokenFilter(SessionRepository sessionRepository,
                              SessionService sessionService,
                              @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.sessionRepository = sessionRepository;
        this.sessionService = sessionService;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // CORS 사전 요청(preflight)에는 토큰이 없다. 보통은 앞단의 CorsFilter가 먼저 응답하지만, 안전장치로 한 번 더 건너뛴다.
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        if (NO_TOKEN_PREFIXES.stream().anyMatch(path::startsWith)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && "/api/v1/sessions".equals(path)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && "/api/v1/sessions/resume".equals(path)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) && PUBLIC_REPORT_PATH.matcher(path).matches()) {
            return true;
        }
        return "GET".equalsIgnoreCase(method) && "/api/v1/personas".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Session session;
        try {
            session = authenticate(request);
        } catch (ApiException e) {
            exceptionResolver.resolveException(request, response, null, e);
            return;
        }

        // 만료 기간은 마지막 접속 + 7일
        sessionService.renewExpiry(session.getId(), session.getExpiresAt());

        try {
            SessionContext.setSessionId(session.getId());
            filterChain.doFilter(request, response);
        } finally {
            SessionContext.clear();
        }
    }

    private Session authenticate(HttpServletRequest request) {
        String token = request.getHeader(SESSION_TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "세션 토큰이 없습니다.");
        }

        Session session = sessionRepository.findBySessionToken(token)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "유효하지 않은 세션 토큰입니다."));

        if (session.isExpired()) {
            throw new ApiException(ErrorCode.SESSION_EXPIRED, "세션이 만료되었습니다.", session.getState().name());
        }

        UUID pathSessionId = extractSessionIdFromPath(request.getRequestURI());
        if (pathSessionId != null && !pathSessionId.equals(session.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "다른 세션에는 접근할 수 없습니다.");
        }
        return session;
    }

    private UUID extractSessionIdFromPath(String path) {
        Matcher matcher = SESSION_ID_IN_PATH.matcher(path);
        if (!matcher.find()) {
            return null;
        }
        String candidate = matcher.group(1);
        if ("resume".equals(candidate)) {
            return null;
        }
        try {
            return UUID.fromString(candidate);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
