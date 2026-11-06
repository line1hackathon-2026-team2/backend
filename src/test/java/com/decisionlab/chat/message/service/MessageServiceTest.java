package com.decisionlab.chat.message.service;

import com.decisionlab.ai.conversation.ConversationEngine;
import com.decisionlab.chat.message.dto.AiMessage;
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
import com.decisionlab.persona.entity.SwitchReason;
import com.decisionlab.persona.repository.PersonaSwitchRepository;
import com.decisionlab.session.entity.Session;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    SessionRepository sessionRepository;
    @Mock
    MessageRepository messageRepository;
    @Mock
    PersonaSwitchRepository personaSwitchRepository;
    @Mock
    ConversationEngine conversationEngine;

    @InjectMocks
    MessageService messageService;

    UUID sessionId = UUID.randomUUID();
    Session session;

    @BeforeEach
    void setUp() {
        session = new Session("token", LocalDateTime.now().plusDays(7));
        session.transitionTo(SessionState.EXPLORING);
    }

    @Test
    void send_throws404WhenSessionMissing() {
        given(sessionRepository.findById(sessionId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.send(sessionId, new SendMessageRequest(InputType.FREE_TEXT, null, "hi")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void send_throws409WhenNotExploring() {
        session.transitionTo(SessionState.CREATED);
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> messageService.send(sessionId, new SendMessageRequest(InputType.FREE_TEXT, null, "hi")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
    }

    @Test
    void send_throwsValidationErrorWhenButtonMissingSelectedOptionId() {
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> messageService.send(sessionId, new SendMessageRequest(InputType.BUTTON, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void send_throwsValidationErrorWhenFreeTextMissingContent() {
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> messageService.send(sessionId, new SendMessageRequest(InputType.FREE_TEXT, null, "  ")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void send_resolvesButtonContentFromLastAiMessageOptionsAndReturnsEngineReply() {
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(personaSwitchRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId))
                .willReturn(Optional.of(PersonaSwitch.builder()
                        .sessionId(sessionId)
                        .toPersonaCode(PersonaCode.ACHIEVEMENT)
                        .reason(SwitchReason.USER_SELECT)
                        .build()));

        Message openingAiMessage = Message.builder()
                .sessionId(sessionId)
                .turnNo(1)
                .role(MessageRole.AI)
                .personaCode(PersonaCode.ACHIEVEMENT)
                .content("질문")
                .options(List.of(new MessageOption("o1", "네, 말해볼게요")))
                .inputType(InputType.BUTTON)
                .build();
        given(messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId)).willReturn(List.of(openingAiMessage));
        given(messageRepository.save(any(Message.class))).willAnswer(invocation -> {
            Message toSave = invocation.getArgument(0);
            setId(toSave, 2L);
            return toSave;
        });
        given(conversationEngine.respond(eq(sessionId), eq(PersonaCode.ACHIEVEMENT), eq("네, 말해볼게요")))
                .willReturn(new AiMessage(3L, PersonaCode.ACHIEVEMENT, "다음 질문", List.of()));

        SendMessageResponse response = messageService.send(sessionId,
                new SendMessageRequest(InputType.BUTTON, "o1", null));

        assertThat(response.userMessageId()).isEqualTo(2L);
        assertThat(response.aiMessage().content()).isEqualTo("다음 질문");
        assertThat(response.canProceedToDecision()).isFalse();
        verify(messageRepository).save(argThat(m -> m != null && "네, 말해볼게요".equals(m.getContent())));
    }

    @Test
    void send_throwsValidationErrorWhenSelectedOptionIdNotFound() {
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(personaSwitchRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId))
                .willReturn(Optional.of(PersonaSwitch.builder()
                        .sessionId(sessionId)
                        .toPersonaCode(PersonaCode.ACHIEVEMENT)
                        .reason(SwitchReason.USER_SELECT)
                        .build()));

        Message openingAiMessage = Message.builder()
                .sessionId(sessionId)
                .turnNo(1)
                .role(MessageRole.AI)
                .personaCode(PersonaCode.ACHIEVEMENT)
                .content("질문")
                .options(List.of(new MessageOption("o1", "네")))
                .inputType(InputType.BUTTON)
                .build();
        given(messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId)).willReturn(List.of(openingAiMessage));

        assertThatThrownBy(() -> messageService.send(sessionId, new SendMessageRequest(InputType.BUTTON, "no-such-option", null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void history_filtersByBeforeTurnAndLimit() {
        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        List<Message> messages = List.of(
                messageWithTurn(1L, 1), messageWithTurn(2L, 2), messageWithTurn(3L, 3), messageWithTurn(4L, 4));
        given(messageRepository.findBySessionIdOrderByTurnNoAsc(sessionId)).willReturn(messages);

        var result = messageService.history(sessionId, 2, 4);

        assertThat(result.messages()).hasSize(2);
        assertThat(result.messages().get(0).turnNo()).isEqualTo(2);
        assertThat(result.messages().get(1).turnNo()).isEqualTo(3);
    }

    private static Message messageWithTurn(Long id, int turnNo) {
        Message message = Message.builder()
                .sessionId(UUID.randomUUID())
                .turnNo(turnNo)
                .role(MessageRole.AI)
                .personaCode(PersonaCode.ACHIEVEMENT)
                .content("content-" + turnNo)
                .options(List.of())
                .inputType(InputType.BUTTON)
                .build();
        setId(message, id);
        return message;
    }

    private static void setId(Message message, Long id) {
        try {
            Field field = Message.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(message, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
