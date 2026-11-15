package com.decisionlab.report.repository;

import com.decisionlab.report.entity.ReportAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportActionRepository extends JpaRepository<ReportAction, Long> {

    List<ReportAction> findByReportIdOrderBySeqAsc(Long reportId);

    void deleteByReportId(Long reportId);
}
