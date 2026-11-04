package com.decisionlab.ai.dilemma;

import com.decisionlab.dilemma.dto.DilemmaResult;

import java.util.UUID;

public interface DilemmaGenerator {

    DilemmaResult generate(UUID sessionId, Long decisionId);
}
