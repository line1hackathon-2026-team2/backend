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
import com.decisionlab.persona.entity.SwitchReason;
import com.decisionlab.persona.repository.PersonaSwitchRepository;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.service.SessionStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    SessionStateService sessionStateService;

    @Mock
    ConversationEngine conversationEngine;

    @Mock
    MessageRepository messageRepository;

    @Mock
    PersonaSwitchRepository personaSwitchRepository;

    @InjectMocks
    MessageService messageService;

    UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(personaSwitchRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).thenReturn(List.of(
                PersonaSwitch.builder()
                        .sessionId(sessionId)
                        .toPersonaCode(PersonaCode.ACHIEVEMENT)
                        .reason(SwitchReason.USER_SELECT)
                        .build()
        ));
    }

    @Test
    void send_freeText_savesUserMessageAndReturnsAiResponse() throws Exception {
        given(messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId)).willReturn(List.of());
        given(messageRepository.save(any())).willAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            setId(message, 1L);
            return message;
        });
        AiMessage aiMessage = new AiMessage(2L, PersonaCode.ACHIEVEMENT, "질문", List.of(), 2);
        given(conversationEngine.respond(sessionId, PersonaCode.ACHIEVEMENT, "제 생각이에요")).willReturn(aiMessage);

        MessageResponse response = messageService.send(sessionId,
                new MessageRequest(InputType.FREE_TEXT, null, "제 생각이에요"));

        verify(sessionStateService).requireState(sessionId, SessionState.EXPLORING);
        assertThat(response.userMessageId()).isEqualTo("1");
        assertThat(response.aiMessage().messageId()).isEqualTo("2");
        assertThat(response.turnNo()).isEqualTo(2);
        assertThat(response.canProceedToDecision()).isFalse();
    }

    @Test
    void send_button_resolvesLabelFromLastAiMessageOptions() {
        Message lastAiMessage = Message.builder()
                .sessionId(sessionId)
                .turnNo(1)
                .role(MessageRole.AI)
                .personaCode(PersonaCode.ACHIEVEMENT)
                .content("질문")
                .options(List.of(new MessageOption("o1", "네, 말해볼게요")))
                .inputType(InputType.BUTTON)
                .build();
        given(messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId)).willReturn(List.of(lastAiMessage));
        given(messageRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        AiMessage aiMessage = new AiMessage(3L, PersonaCode.ACHIEVEMENT, "다음 질문", List.of(), 3);
        given(conversationEngine.respond(sessionId, PersonaCode.ACHIEVEMENT, "네, 말해볼게요")).willReturn(aiMessage);

        MessageResponse response = messageService.send(sessionId,
                new MessageRequest(InputType.BUTTON, "o1", null));

        assertThat(response.turnNo()).isEqualTo(3);
        assertThat(response.canProceedToDecision()).isTrue();
    }

    @Test
    void send_button_withUnknownOptionId_throwsValidationError() {
        Message lastAiMessage = Message.builder()
                .sessionId(sessionId)
                .turnNo(1)
                .role(MessageRole.AI)
                .options(List.of(new MessageOption("o1", "네")))
                .build();
        given(messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId)).willReturn(List.of(lastAiMessage));

        assertThatThrownBy(() -> messageService.send(sessionId, new MessageRequest(InputType.BUTTON, "o9", null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void send_freeText_withBlankContent_throwsValidationError() {
        assertThatThrownBy(() -> messageService.send(sessionId, new MessageRequest(InputType.FREE_TEXT, null, " ")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getHistory_returnsMessagesInAscendingTurnOrder() {
        Message turn1 = Message.builder().sessionId(sessionId).turnNo(1).role(MessageRole.AI).content("A").build();
        Message turn2 = Message.builder().sessionId(sessionId).turnNo(2).role(MessageRole.USER).content("B").build();
        given(messageRepository.findBySessionIdOrderByTurnNoDesc(any(), any())).willReturn(List.of(turn2, turn1));

        MessageHistoryResponse response = messageService.getHistory(sessionId, 50, null);

        assertThat(response.messages()).hasSize(2);
        assertThat(response.messages().get(0).turnNo()).isEqualTo(1);
        assertThat(response.messages().get(1).turnNo()).isEqualTo(2);
    }

    private void setId(Object entity, Long id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
