package com.decisionlab.report.repository;

import com.decisionlab.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findBySessionId(UUID sessionId);
}
