ALTER TABLE platform.component_template
    ADD COLUMN IF NOT EXISTS description varchar(1000),
    ADD COLUMN IF NOT EXISTS min_width integer NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS min_height integer NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS default_width integer NOT NULL DEFAULT 4,
    ADD COLUMN IF NOT EXISTS default_height integer NOT NULL DEFAULT 3;

COMMENT ON COLUMN platform.component_template.description IS '组件模板说明，不得包含危险脚本';
COMMENT ON COLUMN platform.component_template.min_width IS '组件最小宽度网格单位';
COMMENT ON COLUMN platform.component_template.min_height IS '组件最小高度网格单位';
COMMENT ON COLUMN platform.component_template.default_width IS '组件默认宽度网格单位';
COMMENT ON COLUMN platform.component_template.default_height IS '组件默认高度网格单位';

CREATE INDEX IF NOT EXISTS idx_component_template_render_engine
    ON platform.component_template (render_engine);
