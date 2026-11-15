package com.decisionlab.session.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeCodeGeneratorTest {

    private final ResumeCodeGenerator generator = new ResumeCodeGenerator();

    @Test
    void 대문자_4자_하이픈_숫자_4자_형식이다() {
        for (int i = 0; i < 1000; i++) {
            assertThat(generator.generate()).hasSize(9).matches(ResumeCodeGenerator.FORMAT);
        }
    }

    @Test
    void 매번_다른_코드가_나온다() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(generator.generate());
        }
        assertThat(codes).hasSizeGreaterThan(990);
    }
}
