package com.decisionlab.global.config;

import com.decisionlab.session.controller.SessionController;
import com.decisionlab.session.repository.SessionRepository;
import com.decisionlab.session.service.SessionService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorsConfigTest {

    private static final String LOCAL_FRONT = "http://localhost:5173";

    /** application.yaml 기본값(로컬 개발): localhost, 127.0.0.1의 모든 포트 허용 */
    @Nested
    @WebMvcTest(controllers = SessionController.class)
    @Import(WebConfig.class)
    class 로컬_기본값 {

        @Autowired
        MockMvc mockMvc;

        // SessionTokenFilter 의존성
        @MockitoBean
        SessionRepository sessionRepository;

        @MockitoBean
        SessionService sessionService;

        @Test
        void 토큰_없는_사전_요청도_401이_아니라_200이고_CORS_헤더가_붙는다() throws Exception {
            mockMvc.perform(options("/api/v1/sessions/{id}/end", UUID.randomUUID())
                            .header("Origin", LOCAL_FRONT)
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "content-type,x-session-token"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", LOCAL_FRONT))
                    .andExpect(header().string("Access-Control-Allow-Headers", containsStringIgnoringCase("X-Session-Token")))
                    .andExpect(header().string("Access-Control-Max-Age", "3600"))
                    .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));

            verify(sessionRepository, never()).findBySessionToken(any());
        }

        @Test
        void 포트와_관계없이_127_0_0_1도_허용한다() throws Exception {
            mockMvc.perform(options("/api/v1/sessions")
                            .header("Origin", "http://127.0.0.1:3000")
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:3000"));
        }

        @Test
        void 프론트_배포_주소를_허용한다() throws Exception {
            mockMvc.perform(options("/api/v1/sessions")
                            .header("Origin", "https://decision-lab-flax.vercel.app")
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "https://decision-lab-flax.vercel.app"));
        }

        @Test
        void 비슷한_이름의_다른_vercel_주소와_http_주소는_거절한다() throws Exception {
            for (String origin : new String[]{
                    "https://decision-lab-flax-evil.vercel.app",
                    "http://decision-lab-flax.vercel.app"}) {
                mockMvc.perform(options("/api/v1/sessions")
                                .header("Origin", origin)
                                .header("Access-Control-Request-Method", "POST"))
                        .andExpect(status().isForbidden());
            }
        }

        @Test
        void 허용하지_않은_출처의_사전_요청은_거절한다() throws Exception {
            mockMvc.perform(options("/api/v1/sessions")
                            .header("Origin", "https://evil.example.com")
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }

        @Test
        void 쓰지_않는_메서드_DELETE는_거절한다() throws Exception {
            mockMvc.perform(options("/api/v1/sessions/{id}", UUID.randomUUID())
                            .header("Origin", LOCAL_FRONT)
                            .header("Access-Control-Request-Method", "DELETE"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void 토큰_필터의_401_응답에도_CORS_헤더가_붙어_프론트가_에러_코드를_읽을_수_있다() throws Exception {
            mockMvc.perform(get("/api/v1/sessions/{id}", UUID.randomUUID()).header("Origin", LOCAL_FRONT))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("Access-Control-Allow-Origin", LOCAL_FRONT))
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    /** 배포(prod)에서 CORS_ALLOWED_ORIGINS를 아직 넣지 않은 경우: 다른 출처는 모두 거절 */
    @Nested
    @WebMvcTest(controllers = SessionController.class)
    @Import(WebConfig.class)
    @TestPropertySource(properties = "decisionlab.cors.allowed-origin-patterns=")
    class 허용_출처가_비어_있으면 {

        @Autowired
        MockMvc mockMvc;

        @MockitoBean
        SessionRepository sessionRepository;

        @MockitoBean
        SessionService sessionService;

        @Test
        void localhost도_거절한다() throws Exception {
            mockMvc.perform(options("/api/v1/sessions")
                            .header("Origin", LOCAL_FRONT)
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isForbidden());
        }
    }

    /** 배포 주소를 환경변수로 넣은 경우: 그 주소만 허용 */
    @Nested
    @WebMvcTest(controllers = SessionController.class)
    @Import(WebConfig.class)
    @TestPropertySource(properties = "decisionlab.cors.allowed-origin-patterns=https://decision-lab.example.com")
    class 배포_주소를_넣으면 {

        @Autowired
        MockMvc mockMvc;

        @MockitoBean
        SessionRepository sessionRepository;

        @MockitoBean
        SessionService sessionService;

        @Test
        void 그_주소는_허용하고_localhost는_거절한다() throws Exception {
            mockMvc.perform(options("/api/v1/sessions")
                            .header("Origin", "https://decision-lab.example.com")
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "https://decision-lab.example.com"));

            mockMvc.perform(options("/api/v1/sessions")
                            .header("Origin", LOCAL_FRONT)
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isForbidden());
        }
    }
}
