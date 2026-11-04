package com.decisionlab.persona.repository;

import com.decisionlab.persona.entity.Persona;
import com.decisionlab.persona.entity.PersonaCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonaRepository extends JpaRepository<Persona, Long> {

    Optional<Persona> findByCode(PersonaCode code);
}
