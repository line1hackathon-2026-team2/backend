package com.decisionlab.chat.message.dto;

/** #6 메시지 전송에서만 쓰는 반환값. AiMessage는 A의 #5(open)가 그대로 쓰므로 건드리지 않는다. */
public record AiMessageTurn(AiMessage aiMessage, Integer turnNo) {
}
