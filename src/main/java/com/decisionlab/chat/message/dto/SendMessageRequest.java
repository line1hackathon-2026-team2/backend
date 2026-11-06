package com.decisionlab.chat.message.dto;

import com.decisionlab.chat.message.entity.InputType;

public record SendMessageRequest(InputType inputType, String selectedOptionId, String content) {
}
