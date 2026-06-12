ALTER TABLE platform.dashboard
    ADD COLUMN IF NOT EXISTS description varchar(1000),
    ADD COLUMN IF NOT EXISTS screen_width integer NOT NULL DEFAULT 1920,
    ADD COLUMN IF NOT EXISTS screen_height integer NOT NULL DEFAULT 1080,
    ADD COLUMN IF NOT EXISTS background_config_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS theme_config_json jsonb NOT NULL DEFAULT '{}'::jsonb;

COMMENT ON COLUMN platform.dashboard.description IS '大屏说明，不得包含危险脚本';
COMMENT ON COLUMN platform.dashboard.screen_width IS '大屏设计画布宽度';
COMMENT ON COLUMN platform.dashboard.screen_height IS '大屏设计画布高度';
COMMENT ON COLUMN platform.dashboard.background_config_json IS '大屏背景配置 JSON，不得包含业务系统数据或危险脚本';
COMMENT ON COLUMN platform.dashboard.theme_config_json IS '大屏主题配置 JSON，不得包含业务系统数据或危险脚本';

CREATE INDEX IF NOT EXISTS idx_dashboard_code
    ON platform.dashboard (dashboard_code);
