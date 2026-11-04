package com.decisionlab.dilemma.repository;

import com.decisionlab.dilemma.entity.Dilemma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DilemmaRepository extends JpaRepository<Dilemma, Long> {

    Optional<Dilemma> findBySessionId(UUID sessionId);
}
