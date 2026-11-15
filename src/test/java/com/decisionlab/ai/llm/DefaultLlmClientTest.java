package com.decisionlab.ai.llm;

import com.anthropic.core.JsonValue;
import com.anthropic.core.http.Headers;
import com.anthropic.errors.BadRequestException;
import com.decisionlab.ai.prompt.PromptLoader;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultLlmClientTest {

    @Mock
    ChatClient chatClient;

    @Mock
    ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    PromptLoader promptLoader;

    DefaultLlmClient llmClient;

    record Dummy(String value) {
    }

    @BeforeEach
    void setUp() {
        llmClient = new DefaultLlmClient(chatClient, promptLoader);
        given(promptLoader.render(eq("key"), any())).willReturn("prompt text");
        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.user("prompt text")).willReturn(requestSpec);
        given(requestSpec.call()).willReturn(callResponseSpec);
    }

    @Test
    void call_returnsEntityOnFirstSuccess() {
        given(callResponseSpec.entity(Dummy.class)).willReturn(new Dummy("ok"));

        Dummy result = llmClient.call("key", Map.of(), Dummy.class);

        assertThat(result.value()).isEqualTo("ok");
        verify(chatClient, times(1)).prompt();
    }

    @Test
    void call_retriesOnFailureThenSucceeds() {
        given(callResponseSpec.entity(Dummy.class))
                .willThrow(new RuntimeException("일시적 오류"))
                .willReturn(new Dummy("recovered"));

        Dummy result = llmClient.call("key", Map.of(), Dummy.class);

        assertThat(result.value()).isEqualTo("recovered");
        verify(chatClient, times(2)).prompt();
    }

    @Test
    void call_throwsLlmFailed_afterExhaustingRetries() {
        given(callResponseSpec.entity(Dummy.class)).willThrow(new RuntimeException("계속 실패"));

        assertThatThrownBy(() -> llmClient.call("key", Map.of(), Dummy.class))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.LLM_FAILED);

        // 최초 1회 + 재시도 최대 2회 = 총 3회
        verify(chatClient, times(3)).prompt();
    }

    @Test
    void call_doesNotRetry_onNonRetryable400Error() {
        BadRequestException badRequest = BadRequestException.builder()
                .headers(Headers.builder().build())
                .body(JsonValue.from(Map.of()))
                .build();
        given(callResponseSpec.entity(Dummy.class)).willThrow(badRequest);

        assertThatThrownBy(() -> llmClient.call("key", Map.of(), Dummy.class))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.LLM_FAILED);

        // 400은 재시도해도 똑같이 실패하므로 1회만 호출되어야 한다
        verify(chatClient, times(1)).prompt();
    }
}
