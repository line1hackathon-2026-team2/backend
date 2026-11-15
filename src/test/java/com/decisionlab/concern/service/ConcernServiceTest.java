package com.decisionlab.concern.service;

import com.decisionlab.concern.dto.ConcernRequest;
import com.decisionlab.concern.dto.ConcernResponse;
import com.decisionlab.concern.entity.Concern;
import com.decisionlab.concern.entity.ConcernType;
import com.decisionlab.concern.repository.ConcernRepository;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.service.SessionStateService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConcernServiceTest {

    private final ConcernRepository concernRepository = mock(ConcernRepository.class);
    private final SessionStateService sessionStateService = mock(SessionStateService.class);
    private final ConcernService concernService = new ConcernService(concernRepository, sessionStateService);

    private final UUID sessionId = UUID.randomUUID();

    private ConcernRequest request(List<String> options, String preference, String reason) {
        return new ConcernRequest("취업과 대학원 사이 고민", ConcernType.CAREER, options, preference, reason);
    }

    private Concern saveNewAndCapture(ConcernRequest request) {
        when(concernRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
        when(concernRepository.saveAndFlush(any(Concern.class))).thenAnswer(inv -> inv.getArgument(0));
        concernService.saveConcern(sessionId, request);
        ArgumentCaptor<Concern> captor = ArgumentCaptor.forClass(Concern.class);
        verify(concernRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private void assertValidationError(ConcernRequest request) {
        assertThatThrownBy(() -> concernService.saveConcern(sessionId, request))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(concernRepository, never()).saveAndFlush(any());
    }

    @Test
    void 처음_입력하면_모든_필드를_저장하고_concernId를_문자열로_돌려준다() {
        when(concernRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
        when(concernRepository.saveAndFlush(any(Concern.class))).thenAnswer(inv -> {
            Concern saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 7L);
            return saved;
        });

        ConcernResponse response = concernService.saveConcern(sessionId,
                request(List.of("취업", "대학원 진학"), "취업", "전문성을 더 키우고 싶어"));

        ArgumentCaptor<Concern> captor = ArgumentCaptor.forClass(Concern.class);
        verify(concernRepository).saveAndFlush(captor.capture());
        Concern saved = captor.getValue();
        assertThat(saved.getSessionId()).isEqualTo(sessionId);
        assertThat(saved.getConcernType()).isEqualTo(ConcernType.CAREER);
        assertThat(saved.getOptions()).containsExactly("취업", "대학원 진학");
        assertThat(saved.getInitialPreference()).isEqualTo("취업");
        assertThat(saved.getInitialPreferenceReason()).isEqualTo("전문성을 더 키우고 싶어");

        assertThat(response.state()).isEqualTo(SessionState.CREATED);
        assertThat(response.concernId()).isEqualTo("7");
    }

    @Test
    void 이미_있으면_덮어쓰고_새로_저장하지_않는다() {
        Concern existing = Concern.builder()
                .sessionId(sessionId).freeText("이전 고민").concernType(ConcernType.OTHER)
                .options(List.of("A", "B")).initialPreference("A").initialPreferenceReason("이전 이유")
                .build();
        ReflectionTestUtils.setField(existing, "id", 3L);
        when(concernRepository.findBySessionId(sessionId)).thenReturn(Optional.of(existing));

        ConcernResponse response = concernService.saveConcern(sessionId,
                new ConcernRequest("새 고민", ConcernType.MAJOR, null, null, null));

        verify(concernRepository, never()).saveAndFlush(any());
        assertThat(existing.getFreeText()).isEqualTo("새 고민");
        assertThat(existing.getConcernType()).isEqualTo(ConcernType.MAJOR);
        assertThat(existing.getOptions()).isNull();
        assertThat(existing.getInitialPreference()).isNull();
        assertThat(existing.getInitialPreferenceReason()).isNull();
        assertThat(response.concernId()).isEqualTo("3");
    }

    @Test
    void 선택지는_입력_순서대로_앞뒤_공백을_지워_저장한다() {
        Concern saved = saveNewAndCapture(request(List.of(" 취업 ", "대학원 진학", "창업"), null, null));

        assertThat(saved.getOptions()).containsExactly("취업", "대학원 진학", "창업");
    }

    @Test
    void 선택지_빈_목록과_빈_문자열은_입력_안_함으로_저장한다() {
        Concern saved = saveNewAndCapture(request(List.of(), "  ", "  "));

        assertThat(saved.getOptions()).isNull();
        assertThat(saved.getInitialPreference()).isNull();
        assertThat(saved.getInitialPreferenceReason()).isNull();
    }

    @Test
    void 아직_모르겠어요를_골라도_이유는_저장한다() {
        Concern saved = saveNewAndCapture(request(List.of("취업", "대학원 진학"), null, "둘 다 장단점이 있어서"));

        assertThat(saved.getInitialPreference()).isNull();
        assertThat(saved.getInitialPreferenceReason()).isEqualTo("둘 다 장단점이 있어서");
    }

    @Test
    void 선택지가_1개면_400() {
        assertValidationError(request(List.of("취업"), null, null));
    }

    @Test
    void 선택지가_4개면_400() {
        assertValidationError(request(List.of("A", "B", "C", "D"), null, null));
    }

    @Test
    void 선택지가_중복되면_400() {
        assertValidationError(request(List.of("취업", " 취업"), null, null));
    }

    @Test
    void 초기_선호가_선택지에_없으면_400() {
        assertValidationError(request(List.of("취업", "대학원 진학"), "창업", null));
    }

    @Test
    void 선택지_없이_초기_선호만_보내면_400() {
        assertValidationError(request(null, "취업", null));
    }

    @Test
    void 첫_입력이_동시에_들어와_UNIQUE_위반이면_409() {
        when(concernRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());
        when(concernRepository.saveAndFlush(any(Concern.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'uk_concerns_session_id'"));

        assertThatThrownBy(() -> concernService.saveConcern(sessionId, request(null, null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException api = (ApiException) e;
                    assertThat(api.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
                    assertThat(api.getCurrentState()).isEqualTo("CREATED");
                });
    }

    @Test
    void CREATED가_아니면_409이고_저장하지_않는다() {
        doThrow(new ApiException(ErrorCode.INVALID_STATE_TRANSITION, "불가", "EXPLORING"))
                .when(sessionStateService).requireState(sessionId, SessionState.CREATED);

        assertThatThrownBy(() -> concernService.saveConcern(sessionId, request(null, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);

        verify(concernRepository, never()).findBySessionId(any());
        verify(concernRepository, never()).saveAndFlush(any());
    }
}
