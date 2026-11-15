package com.decisionlab.dilemma.entity;

import com.decisionlab.chat.message.entity.MessageOption;
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

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dilemmas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dilemma extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "session_id", nullable = false, length = 36)
    private UUID sessionId;

    @Column(name = "decision_id", nullable = false)
    private Long decisionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dilemma_type", nullable = false, length = 30)
    private DilemmaType dilemmaType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "scenario_text", nullable = false, length = 1000)
    private String scenarioText;

    @Column(name = "verification_goal", nullable = false, length = 300)
    private String verificationGoal;

    /** S08 흔들린 가치 보기(2~3개). 최초 생성 후 저장하여 재호출/새로고침(4.2 shakenValueOptions) 시 재사용 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shaken_value_options")
    private List<MessageOption> shakenValueOptions;

    @Column(name = "is_fallback", nullable = false)
    private boolean fallback;

    @Builder
    public Dilemma(UUID sessionId, Long decisionId, DilemmaType dilemmaType, String title, String scenarioText,
                   String verificationGoal, List<MessageOption> shakenValueOptions, boolean fallback) {
        this.sessionId = sessionId;
        this.decisionId = decisionId;
        this.dilemmaType = dilemmaType;
        this.title = title != null ? title : "";
        this.scenarioText = scenarioText;
        this.verificationGoal = verificationGoal;
        this.shakenValueOptions = shakenValueOptions;
        this.fallback = fallback;
    }

    public void updateShakenValueOptions(List<MessageOption> shakenValueOptions) {
        this.shakenValueOptions = shakenValueOptions;
    }
}
