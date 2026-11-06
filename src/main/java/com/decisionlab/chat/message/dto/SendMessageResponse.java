package com.decisionlab.chat.message.dto;

public record SendMessageResponse(
        Long userMessageId,
        AiMessage aiMessage,
        Integer turnNo,
        boolean canProceedToDecision
) {
}
