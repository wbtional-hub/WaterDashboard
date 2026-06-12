ALTER TABLE platform.data_source_health_log
    ALTER COLUMN data_source_id DROP NOT NULL;

ALTER TABLE platform.data_source_health_log
    ADD COLUMN IF NOT EXISTS test_target_json jsonb NOT NULL DEFAULT '{}'::jsonb;

COMMENT ON COLUMN platform.data_source_health_log.test_target_json IS '临时连接测试的脱敏目标摘要，不得包含密码、Token、完整连接串或密钥';

CREATE INDEX IF NOT EXISTS idx_data_source_health_log_created_at
    ON platform.data_source_health_log (created_at);
