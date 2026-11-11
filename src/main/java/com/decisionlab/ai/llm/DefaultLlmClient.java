package com.decisionlab.ai.llm;

import com.decisionlab.ai.prompt.PromptLoader;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Spring AI {@link ChatClient}(Anthropic Claude) 래퍼.
 * 타임아웃 30초, 실패 시 최대 2회 재시도(총 3회 시도), 모두 실패하면 LLM_FAILED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultLlmClient implements LlmClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final long TIMEOUT_SECONDS = 30;

    private final ChatClient chatClient;
    private final PromptLoader promptLoader;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public <T> T call(String promptKey, Map<String, Object> variables, Class<T> responseType) {
        String prompt = promptLoader.render(promptKey, variables);

        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return callOnce(prompt, responseType);
            } catch (RuntimeException e) {
                lastError = e;
                log.warn("LLM 호출 실패 (promptKey={}, attempt={}/{})", promptKey, attempt, MAX_ATTEMPTS, e);
            }
        }

        throw new ApiException(ErrorCode.LLM_FAILED, "LLM 호출에 실패했습니다.", null);
    }

    private <T> T callOnce(String prompt, Class<T> responseType) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(
                () -> chatClient.prompt().user(prompt).call().entity(responseType), executor);
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new IllegalStateException("LLM 응답이 " + TIMEOUT_SECONDS + "초 내에 오지 않았습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM 호출이 중단되었습니다.", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException("LLM 호출 중 오류가 발생했습니다.", e.getCause());
        }
    }
}
