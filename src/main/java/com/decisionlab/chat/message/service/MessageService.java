package com.decisionlab.chat.message.service;

import com.decisionlab.ai.conversation.ConversationEngine;
import com.decisionlab.chat.message.dto.AiMessage;
import com.decisionlab.chat.message.dto.MessageHistoryResponse;
import com.decisionlab.chat.message.dto.MessageRequest;
import com.decisionlab.chat.message.dto.MessageResponse;
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
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.service.SessionStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    // 명세서 4.6: 약 3~5턴 이후 true를 권장
    private static final int MIN_TURN_FOR_DECISION = 3;
    private static final int DEFAULT_HISTORY_LIMIT = 50;

    private final SessionStateService sessionStateService;
    private final ConversationEngine conversationEngine;
    private final MessageRepository messageRepository;
    // A의 테이블은 읽기만 한다 (현재 자아 조회)
    private final PersonaSwitchRepository personaSwitchRepository;

    /** #6 사용자 발화 전송 + AI 응답 (S04, F03) */
    @Transactional
    public MessageResponse send(UUID sessionId, MessageRequest request) {
        sessionStateService.requireState(sessionId, SessionState.EXPLORING);

        PersonaCode currentPersona = currentPersonaCode(sessionId);
        String userContent = resolveUserContent(sessionId, request);

        Message userMessage = messageRepository.save(Message.builder()
                .sessionId(sessionId)
                .turnNo(nextTurnNo(sessionId))
                .role(MessageRole.USER)
                .personaCode(currentPersona)
                .content(userContent)
                .inputType(request.inputType())
                .selectedOptionId(request.selectedOptionId())
                .build());

        AiMessage aiMessage = conversationEngine.respond(sessionId, currentPersona, userContent);

        boolean canProceedToDecision = aiMessage.turnNo() != null && aiMessage.turnNo() >= MIN_TURN_FOR_DECISION;

        return new MessageResponse(
                String.valueOf(userMessage.getId()),
                MessageResponse.AiMessageItem.from(aiMessage),
                aiMessage.turnNo(),
                canProceedToDecision
        );
    }

    /** #7 대화 이력 조회 */
    @Transactional(readOnly = true)
    public MessageHistoryResponse getHistory(UUID sessionId, Integer limit, Integer beforeTurn) {
        int size = (limit == null || limit <= 0) ? DEFAULT_HISTORY_LIMIT : limit;
        Pageable pageable = PageRequest.of(0, size);

        List<Message> page = beforeTurn != null
                ? messageRepository.findBySessionIdAndTurnNoLessThanOrderByTurnNoDesc(sessionId, beforeTurn, pageable)
                : messageRepository.findBySessionIdOrderByTurnNoDesc(sessionId, pageable);

        List<Message> ascending = new ArrayList<>(page);
        Collections.reverse(ascending);

        return new MessageHistoryResponse(ascending.stream()
                .map(MessageHistoryResponse.MessageItem::from)
                .toList());
    }

    private String resolveUserContent(UUID sessionId, MessageRequest request) {
        InputType inputType = request.inputType();
        if (inputType == InputType.BUTTON) {
            if (request.selectedOptionId() == null || request.selectedOptionId().isBlank()) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "selectedOptionId는 필수입니다.");
            }
            return findOptionLabel(sessionId, request.selectedOptionId());
        }

        String content = request.content();
        if (content == null || content.isBlank() || content.length() > 2000) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "content는 1~2000자여야 합니다.");
        }
        return content;
    }

    private String findOptionLabel(UUID sessionId, String selectedOptionId) {
        List<Message> messages = messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId);
        if (messages.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "선택할 수 있는 이전 질문이 없습니다.");
        }
        Message lastAiMessage = messages.get(messages.size() - 1);
        List<MessageOption> options = lastAiMessage.getOptions();
        if (options == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "선택한 옵션을 찾을 수 없습니다.");
        }
        return options.stream()
                .filter(option -> option.optionId().equals(selectedOptionId))
                .map(MessageOption::label)
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "선택한 옵션을 찾을 수 없습니다."));
    }

    private PersonaCode currentPersonaCode(UUID sessionId) {
        List<PersonaSwitch> switches = personaSwitchRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        if (switches.isEmpty()) {
            throw new ApiException(ErrorCode.NOT_FOUND, "선택된 자아가 없습니다.");
        }
        return switches.get(switches.size() - 1).getToPersonaCode();
    }

    private int nextTurnNo(UUID sessionId) {
        return messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId).size() + 1;
    }
}
