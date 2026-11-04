package com.decisionlab.report.entity;

import com.decisionlab.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "report_actions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportAction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "seq", nullable = false)
    private Integer seq;

    @Column(name = "content", nullable = false, length = 300)
    private String content;

    @Builder
    public ReportAction(Long reportId, Integer seq, String content) {
        this.reportId = reportId;
        this.seq = seq;
        this.content = content;
    }
}
