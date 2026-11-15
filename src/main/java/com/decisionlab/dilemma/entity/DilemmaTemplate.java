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

@Entity
@Table(name = "dilemma_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DilemmaTemplate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "dilemma_type", nullable = false, length = 30)
    private DilemmaType dilemmaType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "scenario_text", nullable = false, length = 1000)
    private String scenarioText;

    @Column(name = "verification_goal", nullable = false, length = 300)
    private String verificationGoal;

    /** S08 흔들린 가치 보기 생성(LLM) 실패 시 폴백용 기본 보기 2개 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shaken_value_options")
    private List<MessageOption> shakenValueOptions;

    @Builder
    public DilemmaTemplate(DilemmaType dilemmaType, String title, String scenarioText,
                           String verificationGoal, List<MessageOption> shakenValueOptions) {
        this.dilemmaType = dilemmaType;
        this.title = title != null ? title : "";
        this.scenarioText = scenarioText;
        this.verificationGoal = verificationGoal;
        this.shakenValueOptions = shakenValueOptions;
    }
}
