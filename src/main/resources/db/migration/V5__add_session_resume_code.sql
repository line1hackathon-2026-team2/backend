-- #17 저장/이어하기: SAVE 시 발급하는 이어하기 코드 (XXXX-9999, 9자). 저장 전에는 NULL
-- 다음 SAVE 때 새 코드로 교체된다. MySQL UNIQUE는 NULL을 여러 개 허용한다.
ALTER TABLE sessions
    ADD COLUMN resume_code CHAR(9) NULL AFTER state,
    ADD UNIQUE KEY uk_sessions_resume_code (resume_code);
