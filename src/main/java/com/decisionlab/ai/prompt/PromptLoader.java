package com.decisionlab.ai.prompt;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * resources/prompts/*.txt 로딩 + {{key}} 변수 치환.
 * promptKey가 "conversation/"로 시작하면 공통 규칙(conversation/common.txt)을 앞에 붙인다.
 */
@Component
public class PromptLoader {

    private static final String CONVERSATION_PREFIX = "conversation/";
    private static final String COMMON_PROMPT_KEY = CONVERSATION_PREFIX + "common";

    public String render(String promptKey, Map<String, Object> variables) {
        return substitute(load(promptKey), variables);
    }

    private String load(String promptKey) {
        if (promptKey.startsWith(CONVERSATION_PREFIX) && !promptKey.equals(COMMON_PROMPT_KEY)) {
            return readPrompt(COMMON_PROMPT_KEY) + "\n\n" + readPrompt(promptKey);
        }
        return readPrompt(promptKey);
    }

    private String readPrompt(String promptKey) {
        String path = "prompts/" + promptKey + ".txt";
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("프롬프트 파일을 읽을 수 없습니다: " + path, e);
        }
    }

    private String substitute(String template, Map<String, Object> variables) {
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            result = result.replace("{{" + entry.getKey() + "}}", value);
        }
        return result;
    }
}
