package com.decisionlab.ai.prompt;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PromptLoaderTest {

    private final PromptLoader promptLoader = new PromptLoader();

    @Test
    void render_substitutesVariablesInPlainPrompt() {
        String result = promptLoader.render("decision-extractor", Map.of(
                "concern", "대학원이냐 취업이냐",
                "history", "성취의 나: 질문\n사용자: 답변"
        ));

        assertThat(result).contains("대학원이냐 취업이냐");
        assertThat(result).contains("성취의 나: 질문\n사용자: 답변");
        assertThat(result).doesNotContain("{{concern}}").doesNotContain("{{history}}");
    }

    @Test
    void render_prependsCommonPromptForConversationKeys() {
        String result = promptLoader.render("conversation/achievement", Map.of(
                "persona_name", "성취의 나",
                "core_value", "성장, 목표, 역량",
                "concern", "고민 내용",
                "context", "지금 상황"
        ));

        // common.txt의 공통 규칙
        assertThat(result).contains("질문은 한 번에 1개만 해.");
        // achievement.txt의 페르소나 전용 지시
        assertThat(result).contains("지금 너는 \"성취의 나\"야.");
        assertThat(result).contains("성장, 목표, 역량");
        assertThat(result).contains("고민 내용");
        assertThat(result).contains("지금 상황");
    }

    @Test
    void render_doesNotDoublePrependCommonPromptForCommonKeyItself() {
        String result = promptLoader.render("conversation/common", Map.of(
                "persona_name", "x",
                "core_value", "y",
                "concern", "z",
                "context", "w"
        ));

        int occurrences = result.split("질문은 한 번에 1개만 해\\.", -1).length - 1;
        assertThat(occurrences).isEqualTo(1);
    }

    @Test
    void render_leavesLiteralPlaceholder_whenVariableMissing() {
        String result = promptLoader.render("decision-extractor", Map.of("concern", "내용"));

        assertThat(result).contains("{{history}}");
    }
}
