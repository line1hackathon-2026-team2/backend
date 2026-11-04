package com.decisionlab.concern.repository;

import com.decisionlab.concern.entity.Concern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConcernRepository extends JpaRepository<Concern, Long> {

    Optional<Concern> findBySessionId(UUID sessionId);
}
