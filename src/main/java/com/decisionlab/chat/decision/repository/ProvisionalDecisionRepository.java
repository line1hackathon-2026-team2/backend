package com.decisionlab.chat.decision.repository;

import com.decisionlab.chat.decision.entity.ProvisionalDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProvisionalDecisionRepository extends JpaRepository<ProvisionalDecision, Long> {

    List<ProvisionalDecision> findBySessionIdOrderByRevisionNoDesc(UUID sessionId);
}
