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
        return builder.defaultOptions(options).build();
    }
}
