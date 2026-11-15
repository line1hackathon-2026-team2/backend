package com.decisionlab.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class WebConfig {

    /**
     * CORS. SessionTokenFilter보다 먼저 실행되도록 가장 높은 순서로 등록한다.
     * - 브라우저의 OPTIONS 사전 요청(preflight)에는 X-Session-Token이 없으므로, 토큰 필터보다 앞에서 응답해야 401이 나지 않는다.
     * - 토큰 필터가 내는 401/403/410 응답에도 CORS 헤더가 붙어야 프론트가 에러 코드를 읽을 수 있다.
     * 허용 출처는 decisionlab.cors.allowed-origin-patterns(쉼표 구분)로 관리한다. 비어 있으면 다른 출처의 요청은 모두 거절된다.
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter(
            @Value("${decisionlab.cors.allowed-origin-patterns:}") List<String> allowedOriginPatterns) {
        CorsConfiguration config = new CorsConfiguration();
        allowedOriginPatterns.stream()
                .map(String::strip)
                .filter(pattern -> !pattern.isEmpty())
                .forEach(config::addAllowedOriginPattern);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-Session-Token"));
        // 쿠키가 아니라 헤더 토큰을 쓰므로 인증정보(쿠키) 전송은 허용하지 않는다
        config.setAllowCredentials(false);
        // 사전 요청 결과를 1시간 캐시해서 요청마다 OPTIONS가 붙지 않게 한다
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
