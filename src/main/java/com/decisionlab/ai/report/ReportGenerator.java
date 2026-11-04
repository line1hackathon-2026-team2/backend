package com.decisionlab.ai.report;

import com.decisionlab.report.dto.ReportResult;
import com.decisionlab.report.entity.EntryPoint;
import com.decisionlab.report.entity.ReportType;

import java.util.UUID;

public interface ReportGenerator {

    ReportResult generate(UUID sessionId, ReportType type, EntryPoint entry);
}
