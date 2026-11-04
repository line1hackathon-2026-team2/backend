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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "persona_switches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonaSwitch extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "session_id", nullable = false, length = 36)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_persona_code", length = 20)
    private PersonaCode fromPersonaCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_persona_code", nullable = false, length = 20)
    private PersonaCode toPersonaCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 20)
    private SwitchReason reason;

    @Builder
    public PersonaSwitch(UUID sessionId, PersonaCode fromPersonaCode, PersonaCode toPersonaCode, SwitchReason reason) {
        this.sessionId = sessionId;
        this.fromPersonaCode = fromPersonaCode;
        this.toPersonaCode = toPersonaCode;
        this.reason = reason;
    }
}
