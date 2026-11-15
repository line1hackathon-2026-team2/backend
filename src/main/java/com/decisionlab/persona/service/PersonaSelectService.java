package com.decisionlab.persona.service;

import com.decisionlab.ai.conversation.ConversationEngine;
import com.decisionlab.chat.message.dto.AiMessage;
import com.decisionlab.global.error.ApiException;
import com.decisionlab.global.error.ErrorCode;
import com.decisionlab.persona.dto.PersonaSelectRequest;
import com.decisionlab.persona.dto.PersonaSelectResponse;
import com.decisionlab.persona.dto.PersonaSelectResponse.OpeningMessage;
import com.decisionlab.persona.entity.PersonaCode;
import com.decisionlab.persona.entity.PersonaSwitch;
import com.decisionlab.persona.entity.SwitchReason;
import com.decisionlab.persona.repository.PersonaSwitchRepository;
import com.decisionlab.session.entity.SessionState;
import com.decisionlab.session.service.SessionStateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * #5 자아 선택/전환 (명세서 4.5)
 * <pre>
 * CREATED   + USER_SELECT          → EXPLORING (최초 선택)
 * EXPLORING + SWITCH / RECONSIDER  → EXPLORING (대화 중 전환 / 딜레마 후 이동)
 * </pre>
 * 기존 대화와 판단 기준은 지우지 않는다. 전환 기록은 persona_switches에 남기고,
 * 현재 자아는 가장 최근 전환 기록으로 계산한다.
 */
@Service
public class PersonaSelectService {

    private final SessionStateService sessionStateService;
    private final PersonaSwitchRepository personaSwitchRepository;
    private final ConversationEngine conversationEngine;

    public PersonaSelectService(SessionStateService sessionStateService,
                                PersonaSwitchRepository personaSwitchRepository,
                                ConversationEngine conversationEngine) {
        this.sessionStateService = sessionStateService;
        this.personaSwitchRepository = personaSwitchRepository;
        this.conversationEngine = conversationEngine;
    }

    // LLM 호출(open)까지 한 트랜잭션이라, 첫 질문 생성이 실패하면 전환 기록과 상태 변경도 함께 롤백된다.
    @Transactional
    public PersonaSelectResponse selectPersona(UUID sessionId, PersonaSelectRequest request) {
        PersonaCode to = request.personaCode();
        SwitchReason reason = request.reason();

        if (!to.isSelectable()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "선택할 수 없는 자아입니다.");
        }

        // 최초 선택은 CREATED에서만, 전환은 EXPLORING에서만 가능하다.
        if (reason == SwitchReason.USER_SELECT) {
            sessionStateService.requireState(sessionId, SessionState.CREATED);
        } else {
            sessionStateService.requireState(sessionId, SessionState.EXPLORING);
        }

        PersonaCode from = personaSwitchRepository.findFirstBySessionIdOrderByIdDesc(sessionId)
                .map(PersonaSwitch::getToPersonaCode)
                .orElse(null);
        if (reason == SwitchReason.SWITCH && to == from) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "이미 대화 중인 자아입니다.");
        }

        personaSwitchRepository.save(PersonaSwitch.builder()
                .sessionId(sessionId)
                .fromPersonaCode(from)
                .toPersonaCode(to)
                .reason(reason)
                .build());

        // 새 자아의 첫 질문. 전환이면 Conversation Engine이 이전 대화를 바탕으로 이어서 묻는다.
        AiMessage opening = conversationEngine.open(sessionId, to);

        sessionStateService.transition(sessionId, SessionState.EXPLORING);

        return new PersonaSelectResponse(SessionState.EXPLORING, to, OpeningMessage.from(opening));
    }
}
