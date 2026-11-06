package com.decisionlab.chat.message.service;

import com.decisionlab.ai.conversation.ConversationEngine;
import com.decisionlab.chat.message.dto.AiMessage;
import com.decisionlab.chat.message.dto.MessageHistoryResponse;
import com.decisionlab.chat.message.dto.MessageHistoryResponse.MessageItem;
import com.decisionlab.chat.message.dto.SendMessageRequest;
import com.decisionlab.chat.message.dto.SendMessageResponse;
import com.decisionlab.chat.message.entity.InputType;
import com.decisionlab.chat.message.entity.Message;
import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.chat.message.entity.MessageRole;
import com.decisionlab.chat.message.repository.MessageRepository;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.persona.entity.PersonaSwitch;
import com.decisionlab.persona.repository.PersonaSwitchRepository;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final int MIN_TURNS_FOR_DECISION = 3;

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final PersonaSwitchRepository personaSwitchRepository;
    private final ConversationEngine conversationEngine;

    @Transactional
    public SendMessageResponse send(UUID sessionId, SendMessageRequest request) {
        Session session = getSession(sessionId);
        requireExploring(session);
        validate(request);

        PersonaCode personaCode = currentPersonaCode(sessionId);
        String userContent = resolveUserContent(sessionId, request);

        int userTurnNo = messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId).size() + 1;
        Message userMessage = messageRepository.save(Message.builder()
                .sessionId(sessionId)
                .turnNo(userTurnNo)
                .role(MessageRole.USER)
                .personaCode(personaCode)
                .content(userContent)
                .inputType(request.inputType())
                .selectedOptionId(request.selectedOptionId())
                .build());

        AiMessage aiMessage = conversationEngine.respond(sessionId, personaCode, userContent);

        int latestTurnNo = countTurns(sessionId);
        boolean canProceedToDecision = latestTurnNo >= MIN_TURNS_FOR_DECISION;

        return new SendMessageResponse(userMessage.getId(), aiMessage, latestTurnNo, canProceedToDecision);
    }

    public MessageHistoryResponse history(UUID sessionId, Integer limit, Integer beforeTurn) {
        getSession(sessionId);

        List<Message> messages = messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId);

        if (beforeTurn != null) {
            messages = messages.stream().filter(m -> m.getTurnNo() < beforeTurn).toList();
        }
        if (limit != null && limit > 0 && messages.size() > limit) {
            messages = messages.subList(messages.size() - limit, messages.size());
        }

        return new MessageHistoryResponse(messages.stream().map(MessageItem::from).toList());
    }

    private void validate(SendMessageRequest request) {
        if (request.inputType() == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "inputType은 필수입니다.");
        }
        if (request.inputType() == InputType.BUTTON
                && (request.selectedOptionId() == null || request.selectedOptionId().isBlank())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "BUTTON 입력에는 selectedOptionId가 필요합니다.");
        }
        if (request.inputType() == InputType.FREE_TEXT
                && (request.content() == null || request.content().isBlank())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "FREE_TEXT 입력에는 content가 필요합니다.");
        }
    }

    private String resolveUserContent(UUID sessionId, SendMessageRequest request) {
        if (request.inputType() == InputType.FREE_TEXT) {
            return request.content();
        }

        Message lastAiMessage = messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId).stream()
                .filter(m -> m.getRole() == MessageRole.AI)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "응답할 AI 메시지가 없습니다."));

        return lastAiMessage.getOptions().stream()
                .filter(option -> option.optionId().equals(request.selectedOptionId()))
                .findFirst()
                .map(MessageOption::label)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "존재하지 않는 선택지입니다."));
    }

    private PersonaCode currentPersonaCode(UUID sessionId) {
        return personaSwitchRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId)
                .map(PersonaSwitch::getToPersonaCode)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "선택된 자아가 없습니다."));
    }

    private int countTurns(UUID sessionId) {
        return messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId).size();
    }

    private void requireExploring(Session session) {
        if (session.getState() != SessionState.EXPLORING) {
            throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION,
                    "현재 상태(" + session.getState() + ")에서는 메시지를 보낼 수 없습니다.", session.getState().name());
        }
    }

    private Session getSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "세션을 찾을 수 없습니다."));
    }
}
