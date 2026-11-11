package com.decisionlab.ai.llm;

import com.anthropic.models.messages.OutputConfig;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        AnthropicChatOptions.Builder options = AnthropicChatOptions.builder();
        // claude-sonnet-5-5는 thinking: {type: "disabled"}를 400으로 거절한다(지원되지 않는 모델에서는
        // "between_tools"로 끄라고 안내하지만 현재 anthropic-java-core에는 해당 타입이 없다).
        // adaptive + effort(LOW)로 사실상 생각 없이 응답하게 하고, maxTokens는 adaptive가 생각을 하게 될
        // 경우를 대비해 넉넉히 둔다.
        options.thinkingAdaptive();
        options.effort(OutputConfig.Effort.LOW);
        options.maxTokens(16000);
        // DefaultLlmClient가 429/5xx 재시도를 직접 담당하므로, SDK 자체의 중복 재시도(기본 2회)는 끈다.
        // 꺼두지 않으면 앱 재시도(최대 3회) x SDK 재시도(최대 3회) = 최악 9회 호출이 나갈 수 있다.
        options.maxRetries(0);
        return builder.defaultOptions(options).build();
    }
}
