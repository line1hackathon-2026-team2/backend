package com.decisionlab.chat.message.controller;

import com.decisionlab.chat.message.dto.MessageHistoryResponse;
import com.decisionlab.chat.message.dto.MessageRequest;
import com.decisionlab.chat.message.dto.MessageResponse;
import com.decisionlab.chat.message.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /** #6 사용자 발화 전송 + AI 응답 (S04) */
    @PostMapping
    public MessageResponse sendMessage(@PathVariable UUID sessionId, @Valid @RequestBody MessageRequest request) {
        return messageService.send(sessionId, request);
    }

    /** #7 대화 이력 조회 (S04) */
    @GetMapping
    public MessageHistoryResponse getMessages(@PathVariable UUID sessionId,
                                              @RequestParam(required = false) Integer limit,
                                              @RequestParam(required = false) Integer beforeTurn) {
        return messageService.getHistory(sessionId, limit, beforeTurn);
    }
}
