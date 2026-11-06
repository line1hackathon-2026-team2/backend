package com.decisionlab.persona.repository;

import com.decisionlab.persona.entity.PersonaSwitch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonaSwitchRepository extends JpaRepository<PersonaSwitch, Long> {

    List<PersonaSwitch> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    Optional<PersonaSwitch> findFirstBySessionIdOrderByCreatedAtDesc(UUID sessionId);
}
