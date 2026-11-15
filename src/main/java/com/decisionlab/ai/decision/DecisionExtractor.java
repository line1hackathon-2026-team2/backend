package com.decisionlab.ai.decision;

import com.decisionlab.chat.decision.dto.DecisionDraftResult;

import java.util.UUID;

public interface DecisionExtractor {

    DecisionDraftResult extract(UUID sessionId);
}
