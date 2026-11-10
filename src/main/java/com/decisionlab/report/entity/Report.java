package com.decisionlab.report.entity;

import com.decisionlab.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "session_id", nullable = false, length = 36)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 30)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_point", length = 10)
    private EntryPoint entryPoint;

    @Column(name = "concern_summary", nullable = false, length = 500)
    private String concernSummary;

    @Column(name = "initial_view", length = 500)
    private String initialView;

    @Column(name = "initial_view_reason", length = 500)
    private String initialViewReason;

    @Column(name = "revised_view", length = 500)
    private String revisedView;

    @Column(name = "revised_view_reason", length = 500)
    private String revisedViewReason;

    @Column(name = "guiding_values", length = 500)
    private String guidingValues;

    @Column(name = "shaking_situation", length = 500)
    private String shakingSituation;

    @Column(name = "shaking_situation_detail", length = 500)
    private String shakingSituationDetail;

    @Column(name = "clarified_criteria", length = 500)
    private String clarifiedCriteria;

    @Column(name = "undecided_reason", length = 500)
    private String undecidedReason;

    @Column(name = "missing_info", length = 500)
    private String missingInfo;

    @Column(name = "disclaimer", nullable = false, length = 300)
    private String disclaimer;

    @Builder
    public Report(UUID sessionId, ReportType reportType, EntryPoint entryPoint, String concernSummary,
                  String initialView, String initialViewReason, String revisedView, String revisedViewReason,
                  String guidingValues, String shakingSituation, String shakingSituationDetail,
                  String clarifiedCriteria, String undecidedReason, String missingInfo, String disclaimer) {
        this.sessionId = sessionId;
        this.reportType = reportType;
        this.entryPoint = entryPoint;
        this.concernSummary = concernSummary;
        this.initialView = initialView;
        this.initialViewReason = initialViewReason;
        this.revisedView = revisedView;
        this.revisedViewReason = revisedViewReason;
        this.guidingValues = guidingValues;
        this.shakingSituation = shakingSituation;
        this.shakingSituationDetail = shakingSituationDetail;
        this.clarifiedCriteria = clarifiedCriteria;
        this.undecidedReason = undecidedReason;
        this.missingInfo = missingInfo;
        this.disclaimer = disclaimer;
    }

    public void update(ReportType reportType, EntryPoint entryPoint, String concernSummary,
                       String initialView, String initialViewReason, String revisedView, String revisedViewReason,
                       String guidingValues, String shakingSituation, String shakingSituationDetail,
                       String clarifiedCriteria, String undecidedReason, String missingInfo, String disclaimer) {
        this.reportType = reportType;
        this.entryPoint = entryPoint;
        this.concernSummary = concernSummary;
        this.initialView = initialView;
        this.initialViewReason = initialViewReason;
        this.revisedView = revisedView;
        this.revisedViewReason = revisedViewReason;
        this.guidingValues = guidingValues;
        this.shakingSituation = shakingSituation;
        this.shakingSituationDetail = shakingSituationDetail;
        this.clarifiedCriteria = clarifiedCriteria;
        this.undecidedReason = undecidedReason;
        this.missingInfo = missingInfo;
        this.disclaimer = disclaimer;
    }
}
