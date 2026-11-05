-- 전체 엔티티 기준 초기 스키마 (12개 테이블, FK 충돌 방지를 위해 한 번에 생성)
-- 담당: A / 초안 작성: B (DDL이 비어있어 로컬 실행이 막혀 있어서 임시로 뽑음, A 리뷰 후 조정 필요)

-- A: 세션 · 상태 전이 · 공통
CREATE TABLE sessions (
    id              CHAR(36)        NOT NULL,
    session_token   VARCHAR(512)    NOT NULL,
    state           VARCHAR(30)     NOT NULL,
    expires_at      DATETIME(6)     NOT NULL,
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sessions_session_token (session_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE concerns (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    session_id          CHAR(36)        NOT NULL,
    free_text           VARCHAR(1000)   NOT NULL,
    concern_type        VARCHAR(20)     NOT NULL,
    initial_preference  VARCHAR(255)    NULL,
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_concerns_session_id (session_id),
    CONSTRAINT fk_concerns_session FOREIGN KEY (session_id) REFERENCES sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE personas (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    code            VARCHAR(20)     NOT NULL,
    name            VARCHAR(50)     NOT NULL,
    core_value      VARCHAR(100)    NOT NULL,
    sample_question VARCHAR(300)    NOT NULL,
    theme_color     VARCHAR(20)     NOT NULL,
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_personas_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE persona_switches (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    session_id          CHAR(36)        NOT NULL,
    from_persona_code   VARCHAR(20)     NULL,
    to_persona_code     VARCHAR(20)     NOT NULL,
    reason              VARCHAR(20)     NOT NULL,
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_persona_switches_session_id (session_id),
    CONSTRAINT fk_persona_switches_session FOREIGN KEY (session_id) REFERENCES sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- B: 1:1 대화 · 잠정 결정 · LLM 래퍼
CREATE TABLE messages (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    session_id          CHAR(36)        NOT NULL,
    turn_no             INT             NOT NULL,
    role                VARCHAR(10)     NOT NULL,
    persona_code        VARCHAR(20)     NULL,
    content             VARCHAR(2000)   NOT NULL,
    options             JSON            NULL,
    input_type          VARCHAR(20)     NULL,
    selected_option_id  VARCHAR(50)     NULL,
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_messages_session_turn (session_id, turn_no),
    CONSTRAINT fk_messages_session FOREIGN KEY (session_id) REFERENCES sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE provisional_decisions (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    session_id              CHAR(36)        NOT NULL,
    revision_no             INT             NOT NULL,
    summary_text            VARCHAR(1000)   NOT NULL,
    decision_text           VARCHAR(200)    NULL,
    core_value              VARCHAR(200)    NULL,
    reason                  VARCHAR(500)    NULL,
    acceptable_cost         VARCHAR(500)    NULL,
    unchecked_condition     VARCHAR(500)    NULL,
    resolved_action         VARCHAR(20)     NULL,
    created_at              DATETIME(6)     NOT NULL,
    updated_at              DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_provisional_decisions_session_id (session_id),
    CONSTRAINT fk_provisional_decisions_session FOREIGN KEY (session_id) REFERENCES sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE judgment_criteria (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    session_id      CHAR(36)        NOT NULL,
    decision_id     BIGINT          NOT NULL,
    category        VARCHAR(50)     NOT NULL,
    content         VARCHAR(500)    NOT NULL,
    source          VARCHAR(20)     NOT NULL,
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_judgment_criteria_session_id (session_id),
    KEY idx_judgment_criteria_decision_id (decision_id),
    CONSTRAINT fk_judgment_criteria_session FOREIGN KEY (session_id) REFERENCES sessions (id),
    CONSTRAINT fk_judgment_criteria_decision FOREIGN KEY (decision_id) REFERENCES provisional_decisions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- C: 딜레마 · 사후 분기 · 리포트
CREATE TABLE dilemmas (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    session_id          CHAR(36)        NOT NULL,
    decision_id         BIGINT          NOT NULL,
    dilemma_type        VARCHAR(30)     NOT NULL,
    scenario_text       VARCHAR(1000)   NOT NULL,
    verification_goal   VARCHAR(300)    NOT NULL,
    is_fallback         TINYINT(1)      NOT NULL,
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    -- 딜레마는 세션당 1개(README 결정 사항) -> DB 레벨에서도 강제. A/C 리뷰에서 맞는지 확인 필요
    UNIQUE KEY uk_dilemmas_session_id (session_id),
    KEY idx_dilemmas_decision_id (decision_id),
    CONSTRAINT fk_dilemmas_session FOREIGN KEY (session_id) REFERENCES sessions (id),
    CONSTRAINT fk_dilemmas_decision FOREIGN KEY (decision_id) REFERENCES provisional_decisions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE dilemma_templates (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    dilemma_type        VARCHAR(30)     NOT NULL,
    scenario_text       VARCHAR(1000)   NOT NULL,
    verification_goal   VARCHAR(300)    NOT NULL,
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE dilemma_responses (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    dilemma_id      BIGINT          NOT NULL,
    choice          VARCHAR(20)     NOT NULL,
    keep_reason     VARCHAR(500)    NULL,
    shaken_value    VARCHAR(500)    NULL,
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_dilemma_responses_dilemma_id (dilemma_id),
    CONSTRAINT fk_dilemma_responses_dilemma FOREIGN KEY (dilemma_id) REFERENCES dilemmas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reports (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    session_id          CHAR(36)        NOT NULL,
    report_type         VARCHAR(30)     NOT NULL,
    entry_point         VARCHAR(10)     NULL,
    concern_summary     VARCHAR(500)    NOT NULL,
    initial_view        VARCHAR(500)    NULL,
    revised_view        VARCHAR(500)    NULL,
    guiding_values      VARCHAR(500)    NULL,
    shaking_situation   VARCHAR(500)    NULL,
    clarified_criteria  VARCHAR(500)    NULL,
    undecided_reason    VARCHAR(500)    NULL,
    missing_info        VARCHAR(500)    NULL,
    disclaimer          VARCHAR(300)    NOT NULL,
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_reports_session_id (session_id),
    CONSTRAINT fk_reports_session FOREIGN KEY (session_id) REFERENCES sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE report_actions (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    report_id   BIGINT          NOT NULL,
    seq         INT             NOT NULL,
    content     VARCHAR(300)    NOT NULL,
    created_at  DATETIME(6)     NOT NULL,
    updated_at  DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_report_actions_report_id (report_id),
    CONSTRAINT fk_report_actions_report FOREIGN KEY (report_id) REFERENCES reports (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
