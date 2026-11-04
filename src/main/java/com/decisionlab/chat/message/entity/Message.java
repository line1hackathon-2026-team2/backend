package com.decisionlab.chat.message.entity;

import com.decisionlab.global.entity.BaseTimeEntity;
import com.decisionlab.persona.entity.PersonaCode;
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
@Table(name = "messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "session_id", nullable = false, length = 36)
    private UUID sessionId;

    @Column(name = "turn_no", nullable = false)
    private Integer turnNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private MessageRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "persona_code", length = 20)
    private PersonaCode personaCode;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options")
    private List<MessageOption> options;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", length = 20)
    private InputType inputType;

    @Column(name = "selected_option_id", length = 50)
    private String selectedOptionId;

    @Builder
    public Message(UUID sessionId, Integer turnNo, MessageRole role, PersonaCode personaCode, String content,
                    List<MessageOption> options, InputType inputType, String selectedOptionId) {
        this.sessionId = sessionId;
        this.turnNo = turnNo;
        this.role = role;
        this.personaCode = personaCode;
        this.content = content;
        this.options = options;
        this.inputType = inputType;
        this.selectedOptionId = selectedOptionId;
    }
}
