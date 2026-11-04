package com.decisionlab.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SESSION_TOKEN_SCHEME = "session_token";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Decision Lab API")
                        .description("AI 자아와 대화하며 의사결정을 검증하는 서비스의 API 명세")
                        .version("v0.1"))
                .components(new Components()
                        .addSecuritySchemes(SESSION_TOKEN_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Session-Token")))
                .addSecurityItem(new SecurityRequirement().addList(SESSION_TOKEN_SCHEME));
    }
}
