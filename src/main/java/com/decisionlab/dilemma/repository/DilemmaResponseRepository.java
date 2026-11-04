package com.decisionlab.dilemma.repository;

import com.decisionlab.dilemma.entity.DilemmaResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DilemmaResponseRepository extends JpaRepository<DilemmaResponse, Long> {

    Optional<DilemmaResponse> findByDilemmaId(Long dilemmaId);
}
