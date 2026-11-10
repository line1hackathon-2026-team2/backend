-- S06/S08 딜레마 제목(title) 및 흔들린 가치 선택지(shaken_value_options) 추가
-- S09 리포트 단계별 부가 설명(initial_view_reason, revised_view_reason, shaking_situation_detail) 추가

-- 기존 시드 행이 있을 경우 NOT NULL 추가 시 충돌하지 않도록 비운 뒤 DilemmaTemplateSeeder가 다시 채우게 한다.
DELETE FROM dilemma_templates;

ALTER TABLE dilemmas
    ADD COLUMN title                 VARCHAR(200) NOT NULL DEFAULT '' AFTER dilemma_type,
    ADD COLUMN shaken_value_options  JSON         NULL AFTER verification_goal;

ALTER TABLE dilemma_templates
    ADD COLUMN title                 VARCHAR(200) NOT NULL DEFAULT '' AFTER dilemma_type,
    ADD COLUMN shaken_value_options  JSON         NULL AFTER verification_goal;

ALTER TABLE reports
    ADD COLUMN initial_view_reason      VARCHAR(500) NULL AFTER initial_view,
    ADD COLUMN revised_view_reason      VARCHAR(500) NULL AFTER revised_view,
    ADD COLUMN shaking_situation_detail VARCHAR(500) NULL AFTER shaking_situation;
