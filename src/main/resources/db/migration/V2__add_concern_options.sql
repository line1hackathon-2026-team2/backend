-- S02 고민 입력: 비교 중인 선택지(2~3개, 각 15자)와 초기 선호의 이유(100자) 추가
ALTER TABLE concerns
    ADD COLUMN options                   JSON         NULL AFTER concern_type,
    ADD COLUMN initial_preference_reason VARCHAR(100) NULL AFTER initial_preference;
