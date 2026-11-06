package com.decisionlab.session.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTokenGeneratorTest {

    private final SessionTokenGenerator generator = new SessionTokenGenerator();

    @Test
    void 토큰은_43자_Base64URL_문자열이다() {
        String token = generator.generate();

        assertThat(token).hasSize(43);
        assertThat(token).matches("^[A-Za-z0-9_-]+$");
    }

    @Test
    void 매번_다른_토큰을_생성한다() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            tokens.add(generator.generate());
        }

        assertThat(tokens).hasSize(1000);
    }
}
