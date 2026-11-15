package com.decisionlab.persona.repository;

import com.decisionlab.persona.entity.PersonaSwitch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonaSwitchRepository extends JpaRepository<PersonaSwitch, Long> {

    List<PersonaSwitch> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    /** 현재 자아 = 가장 최근 전환 기록. 같은 시각에 기록돼도 순서가 정해지도록 id로 정렬한다. */
    Optional<PersonaSwitch> findFirstBySessionIdOrderByIdDesc(UUID sessionId);
}
