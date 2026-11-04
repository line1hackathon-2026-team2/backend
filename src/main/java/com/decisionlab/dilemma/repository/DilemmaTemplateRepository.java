package com.decisionlab.dilemma.repository;

import com.decisionlab.dilemma.entity.DilemmaTemplate;
import com.decisionlab.dilemma.entity.DilemmaType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DilemmaTemplateRepository extends JpaRepository<DilemmaTemplate, Long> {

    Optional<DilemmaTemplate> findByDilemmaType(DilemmaType dilemmaType);
}
