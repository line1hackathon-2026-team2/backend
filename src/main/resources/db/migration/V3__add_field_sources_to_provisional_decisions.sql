-- S05 필드별 출처 배지: decisionText/coreValue/reason/acceptableCost의 출처(USER_STATED/AI_INFERRED)를 저장한다.
-- uncheckedCondition은 정의상 항상 NEEDS_CHECK라 컬럼을 두지 않고 응답 생성 시 고정값으로 내려준다.
ALTER TABLE provisional_decisions
    ADD COLUMN decision_text_source    VARCHAR(20) AFTER decision_text,
    ADD COLUMN core_value_source       VARCHAR(20) AFTER core_value,
    ADD COLUMN reason_source           VARCHAR(20) AFTER reason,
    ADD COLUMN acceptable_cost_source  VARCHAR(20) AFTER acceptable_cost;
