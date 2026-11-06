package com.decisionlab.chat.message.controller;

import com.decisionlab.chat.message.dto.MessageHistoryResponse;
import com.decisionlab.chat.message.dto.SendMessageRequest;
import com.decisionlab.chat.message.dto.SendMessageResponse;
import com.decisionlab.chat.message.service.MessageService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public SendMessageResponse send(@PathVariable UUID sessionId, @RequestBody SendMessageRequest request) {
        return messageService.send(sessionId, request);
    }

    @GetMapping
    public MessageHistoryResponse history(@PathVariable UUID sessionId,
                                           @RequestParam(required = false) Integer limit,
                                           @RequestParam(required = false) Integer beforeTurn) {
        return messageService.history(sessionId, limit, beforeTurn);
    }
}
