package com.decisionlab.ai.llm;

import com.anthropic.errors.AnthropicServiceException;
import com.decisionlab.ai.prompt.PromptLoader;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Spring AI {@link ChatClient}(Anthropic Claude) 래퍼.
 * 타임아웃 30초. 429/5xx/타임아웃처럼 재시도하면 성공할 수 있는 오류만 최대 2회 재시도(총 3회 시도)하고,
 * 400처럼 재시도해도 똑같이 실패하는 오류는 즉시 LLM_FAILED로 던진다.
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

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return callOnce(prompt, responseType);
            } catch (RuntimeException e) {
                log.warn("LLM 호출 실패 (promptKey={}, attempt={}/{})", promptKey, attempt, MAX_ATTEMPTS, e);
                if (!isRetryable(e)) {
                    break;
                }
            }
        }

        throw new ApiException(ErrorCode.LLM_FAILED, "LLM 호출에 실패했습니다.", null);
    }

    private <T> T callOnce(String prompt, Class<T> responseType) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(
                () -> chatClient.prompt().user(prompt).call().entity(responseType), executor);
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IllegalStateException("LLM 응답이 " + TIMEOUT_SECONDS + "초 내에 오지 않았습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM 호출이 중단되었습니다.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("LLM 호출 중 오류가 발생했습니다.", e.getCause());
        }
    }

    /**
     * 400/401/403/404/422 같은, 재시도해도 똑같이 실패하는 4xx로 확인된 경우에만 재시도를 포기한다.
     * 상태 코드를 알 수 없는 오류(타임아웃, 네트워크 오류 등)는 기존처럼 재시도 대상으로 둔다.
     */
    private boolean isRetryable(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof AnthropicServiceException ase) {
                return ase.statusCode() == 429 || ase.statusCode() >= 500;
            }
        }
        return true;
    }
}
