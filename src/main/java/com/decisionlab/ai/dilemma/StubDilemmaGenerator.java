package com.decisionlab.ai.dilemma;

import com.decisionlab.dilemma.dto.DilemmaResult;
import com.decisionlab.dilemma.entity.Dilemma;
import com.decisionlab.dilemma.entity.DilemmaType;
import com.decisionlab.dilemma.repository.DilemmaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StubDilemmaGenerator implements DilemmaGenerator {

    private final DilemmaRepository dilemmaRepository;

    @Override
    @Transactional
    public DilemmaResult generate(UUID sessionId, Long decisionId) {
        Dilemma dilemma = dilemmaRepository.findBySessionId(sessionId)
                .orElseGet(() -> dilemmaRepository.save(Dilemma.builder()
                        .sessionId(sessionId)
                        .decisionId(decisionId)
                        .dilemmaType(DilemmaType.ALT_PATH)
                        .scenarioText("[가상 상황] 희망하던 기업에서 전문 연구를 수행할 수 있는 연구개발 직무를 제안했습니다. 학위 없이도 원하는 역량을 쌓을 수 있다면, 지금의 판단은 달라질까요?")
                        .verificationGoal("전문성을 얻는 방법이 현재 선택한 경로여야만 하는지 검증")
                        .fallback(true)
                        .build()));

        return new DilemmaResult(
                dilemma.getId(),
                dilemma.getDilemmaType(),
                dilemma.getScenarioText(),
                dilemma.getVerificationGoal(),
                dilemma.isFallback()
        );
    }
}
