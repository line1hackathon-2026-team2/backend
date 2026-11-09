package com.decisionlab.chat.decision.service;

import com.decisionlab.chat.decision.dto.EditFieldRequest;
import com.decisionlab.chat.decision.dto.EditFieldResponse;
import com.decisionlab.chat.decision.entity.ProvisionalDecision;
import com.decisionlab.chat.decision.repository.ProvisionalDecisionRepository;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.service.SessionStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    @Mock
    SessionStateService sessionStateService;

    @Mock
    ProvisionalDecisionRepository provisionalDecisionRepository;

    @InjectMocks
    DecisionService decisionService;

    UUID sessionId = UUID.randomUUID();
    Long decisionId = 10L;

    private ProvisionalDecision decision(UUID ownerSessionId) throws Exception {
        ProvisionalDecision decision = ProvisionalDecision.builder()
                .sessionId(ownerSessionId)
                .revisionNo(1)
                .summaryText("요약")
                .decisionText("대학원 진학")
                .coreValue("연구 전문성")
                .reason("연구개발 분야 희망")
                .acceptableCost("취업 시점 지연")
                .uncheckedCondition("실제 연구 경험")
                .build();
        setId(decision, decisionId);
        return decision;
    }

    @Test
    void editField_updatesOnlyRequestedField_keepsRevisionNo() throws Exception {
        given(provisionalDecisionRepository.findById(decisionId)).willReturn(Optional.of(decision(sessionId)));

        EditFieldResponse response = decisionService.editField(sessionId, decisionId,
                new EditFieldRequest("coreValue", "경제적 안정"));

        verify(sessionStateService).requireState(sessionId, SessionState.PROVISIONAL_DECISION);
        assertThat(response.decisionId()).isEqualTo("10");
        assertThat(response.revisionNo()).isEqualTo(1);
        assertThat(response.fields().coreValue()).isEqualTo("경제적 안정");
        assertThat(response.fields().decisionText()).isEqualTo("대학원 진학");
    }

    @Test
    void editField_withUnknownField_throwsValidationError() {
        assertThatThrownBy(() -> decisionService.editField(sessionId, decisionId,
                new EditFieldRequest("unknownField", "값")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void editField_withDecisionFromAnotherSession_throwsNotFound() throws Exception {
        given(provisionalDecisionRepository.findById(decisionId)).willReturn(Optional.of(decision(UUID.randomUUID())));

        assertThatThrownBy(() -> decisionService.editField(sessionId, decisionId,
                new EditFieldRequest("coreValue", "경제적 안정")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    private void setId(Object entity, Long id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
