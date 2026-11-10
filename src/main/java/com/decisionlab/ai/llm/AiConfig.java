package com.decisionlab.ai.llm;

import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        AnthropicChatOptions.Builder options = AnthropicChatOptions.builder();
        // 구조화 출력(JSON)을 받아야 하므로 thinking은 끄고, 긴 대화 이력에도 응답이 잘리지 않도록 maxTokens를 넉넉히 둔다
        options.thinkingDisabled();
        options.maxTokens(2000);
        return builder.defaultOptions(options).build();
    }
}
