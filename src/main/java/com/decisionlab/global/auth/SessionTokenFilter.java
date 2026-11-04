package com.decisionlab.global.auth;

import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.repository.SessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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

    private final SessionRepository sessionRepository;

    public SessionTokenFilter(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (NO_TOKEN_PREFIXES.stream().anyMatch(path::startsWith)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && "/api/v1/sessions".equals(path)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && "/api/v1/sessions/resume".equals(path)) {
            return true;
        }
        return "GET".equalsIgnoreCase(method) && "/api/v1/personas".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
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

        try {
            SessionContext.setSessionId(session.getId());
            filterChain.doFilter(request, response);
        } finally {
            SessionContext.clear();
        }
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
