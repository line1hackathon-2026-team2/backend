package com.decisionlab.persona.service;

import com.decisionlab.ai.conversation.ConversationEngine;
import com.decisionlab.chat.message.dto.AiMessage;
import com.decisionlab.chat.message.entity.MessageOption;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.persona.dto.PersonaSelectRequest;
import com.decisionlab.persona.dto.PersonaSelectResponse;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.persona.entity.PersonaSwitch;
import com.decisionlab.persona.entity.SwitchReason;
import com.decisionlab.persona.repository.PersonaSwitchRepository;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.service.SessionStateService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PersonaSelectServiceTest {

    private final SessionStateService sessionStateService = mock(SessionStateService.class);
    private final PersonaSwitchRepository personaSwitchRepository = mock(PersonaSwitchRepository.class);
    private final ConversationEngine conversationEngine = mock(ConversationEngine.class);
    private final PersonaSelectService service =
            new PersonaSelectService(sessionStateService, personaSwitchRepository, conversationEngine);

    private final UUID sessionId = UUID.randomUUID();

    private AiMessage opening(PersonaCode code) {
        return new AiMessage(42L, code, "첫 질문", List.of(new MessageOption("o1", "맞아")));
    }

    private void currentPersona(PersonaCode code) {
        PersonaSwitch last = PersonaSwitch.builder()
                .sessionId(sessionId).toPersonaCode(code).reason(SwitchReason.USER_SELECT).build();
        when(personaSwitchRepository.findFirstBySessionIdOrderByIdDesc(sessionId)).thenReturn(Optional.of(last));
    }

    private PersonaSwitch savedSwitch() {
        ArgumentCaptor<PersonaSwitch> captor = ArgumentCaptor.forClass(PersonaSwitch.class);
        verify(personaSwitchRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void 최초_선택은_CREATED에서_EXPLORING으로_가고_첫_질문을_돌려준다() {
        when(personaSwitchRepository.findFirstBySessionIdOrderByIdDesc(sessionId)).thenReturn(Optional.empty());
        when(conversationEngine.open(sessionId, PersonaCode.ACHIEVEMENT)).thenReturn(opening(PersonaCode.ACHIEVEMENT));

        PersonaSelectResponse response = service.selectPersona(sessionId,
                new PersonaSelectRequest(PersonaCode.ACHIEVEMENT, SwitchReason.USER_SELECT));

        verify(sessionStateService).requireState(sessionId, SessionState.CREATED);
        PersonaSwitch saved = savedSwitch();
        assertThat(saved.getFromPersonaCode()).isNull();
        assertThat(saved.getToPersonaCode()).isEqualTo(PersonaCode.ACHIEVEMENT);
        assertThat(saved.getReason()).isEqualTo(SwitchReason.USER_SELECT);

        assertThat(response.state()).isEqualTo(SessionState.EXPLORING);
        assertThat(response.personaCode()).isEqualTo(PersonaCode.ACHIEVEMENT);
        assertThat(response.openingMessage().messageId()).isEqualTo("42");
        assertThat(response.openingMessage().content()).isEqualTo("첫 질문");
        assertThat(response.openingMessage().options()).extracting(MessageOption::optionId).containsExactly("o1");
    }

    @Test
    void 기록_저장_첫_질문_상태_전이_순서로_처리한다() {
        when(personaSwitchRepository.findFirstBySessionIdOrderByIdDesc(sessionId)).thenReturn(Optional.empty());
        when(conversationEngine.open(sessionId, PersonaCode.STABILITY)).thenReturn(opening(PersonaCode.STABILITY));

        service.selectPersona(sessionId, new PersonaSelectRequest(PersonaCode.STABILITY, SwitchReason.USER_SELECT));

        InOrder order = inOrder(sessionStateService, personaSwitchRepository, conversationEngine);
        order.verify(sessionStateService).requireState(sessionId, SessionState.CREATED);
        order.verify(personaSwitchRepository).save(any(PersonaSwitch.class));
        order.verify(conversationEngine).open(sessionId, PersonaCode.STABILITY);
        order.verify(sessionStateService).transition(sessionId, SessionState.EXPLORING);
    }

    @Test
    void 대화_중_전환은_EXPLORING에서만_가능하고_이전_자아를_기록한다() {
        currentPersona(PersonaCode.ACHIEVEMENT);
        when(conversationEngine.open(sessionId, PersonaCode.STABILITY)).thenReturn(opening(PersonaCode.STABILITY));

        PersonaSelectResponse response = service.selectPersona(sessionId,
                new PersonaSelectRequest(PersonaCode.STABILITY, SwitchReason.SWITCH));

        verify(sessionStateService).requireState(sessionId, SessionState.EXPLORING);
        PersonaSwitch saved = savedSwitch();
        assertThat(saved.getFromPersonaCode()).isEqualTo(PersonaCode.ACHIEVEMENT);
        assertThat(saved.getToPersonaCode()).isEqualTo(PersonaCode.STABILITY);
        assertThat(saved.getReason()).isEqualTo(SwitchReason.SWITCH);
        verify(sessionStateService).transition(sessionId, SessionState.EXPLORING);
        assertThat(response.personaCode()).isEqualTo(PersonaCode.STABILITY);
    }

    @Test
    void 딜레마_후_이동은_같은_자아로도_가능하다() {
        currentPersona(PersonaCode.EXPLORATION);
        when(conversationEngine.open(sessionId, PersonaCode.EXPLORATION)).thenReturn(opening(PersonaCode.EXPLORATION));

        service.selectPersona(sessionId, new PersonaSelectRequest(PersonaCode.EXPLORATION, SwitchReason.RECONSIDER));

        verify(sessionStateService).requireState(sessionId, SessionState.EXPLORING);
        assertThat(savedSwitch().getReason()).isEqualTo(SwitchReason.RECONSIDER);
    }

    @Test
    void 지금_자아로_SWITCH하면_400() {
        currentPersona(PersonaCode.ACHIEVEMENT);

        assertThatThrownBy(() -> service.selectPersona(sessionId,
                new PersonaSelectRequest(PersonaCode.ACHIEVEMENT, SwitchReason.SWITCH)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(personaSwitchRepository, never()).save(any());
        verifyNoInteractions(conversationEngine);
    }

    @Test
    void 딜레마_관리자는_선택할_수_없다() {
        assertThatThrownBy(() -> service.selectPersona(sessionId,
                new PersonaSelectRequest(PersonaCode.DILEMMA_MANAGER, SwitchReason.USER_SELECT)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(sessionStateService, personaSwitchRepository, conversationEngine);
    }

    @Test
    void 이미_EXPLORING인데_최초_선택이면_409() {
        doThrow(new ApiException(ErrorCode.INVALID_STATE_TRANSITION, "불가", "EXPLORING"))
                .when(sessionStateService).requireState(sessionId, SessionState.CREATED);

        assertThatThrownBy(() -> service.selectPersona(sessionId,
                new PersonaSelectRequest(PersonaCode.ACHIEVEMENT, SwitchReason.USER_SELECT)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
        verify(personaSwitchRepository, never()).save(any());
        verifyNoInteractions(conversationEngine);
    }

    @Test
    void CREATED에서_전환하면_409() {
        doThrow(new ApiException(ErrorCode.INVALID_STATE_TRANSITION, "불가", "CREATED"))
                .when(sessionStateService).requireState(sessionId, SessionState.EXPLORING);

        assertThatThrownBy(() -> service.selectPersona(sessionId,
                new PersonaSelectRequest(PersonaCode.STABILITY, SwitchReason.SWITCH)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
        verifyNoInteractions(conversationEngine);
    }

    @Test
    void 첫_질문_생성이_실패하면_상태를_바꾸지_않는다() {
        when(personaSwitchRepository.findFirstBySessionIdOrderByIdDesc(sessionId)).thenReturn(Optional.empty());
        when(conversationEngine.open(sessionId, PersonaCode.ACHIEVEMENT))
                .thenThrow(new ApiException(ErrorCode.LLM_FAILED, "LLM 호출 실패"));

        assertThatThrownBy(() -> service.selectPersona(sessionId,
                new PersonaSelectRequest(PersonaCode.ACHIEVEMENT, SwitchReason.USER_SELECT)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LLM_FAILED);
        verify(sessionStateService, never()).transition(any(), any());
    }
}
