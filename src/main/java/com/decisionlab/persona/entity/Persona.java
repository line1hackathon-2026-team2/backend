package com.decisionlab.persona.entity;

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

@Entity
@Table(name = "personas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Persona extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private PersonaCode code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "core_value", nullable = false, length = 100)
    private String coreValue;

    @Column(name = "sample_question", nullable = false, length = 300)
    private String sampleQuestion;

    @Column(name = "theme_color", nullable = false, length = 20)
    private String themeColor;

    @Builder
    public Persona(PersonaCode code, String name, String coreValue, String sampleQuestion, String themeColor) {
        this.code = code;
        this.name = name;
        this.coreValue = coreValue;
        this.sampleQuestion = sampleQuestion;
        this.themeColor = themeColor;
    }
}
