package com.decisionlab.chat.criterion.repository;

import com.decisionlab.chat.criterion.entity.JudgmentCriterion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JudgmentCriterionRepository extends JpaRepository<JudgmentCriterion, Long> {

    List<JudgmentCriterion> findByDecisionId(Long decisionId);
}
