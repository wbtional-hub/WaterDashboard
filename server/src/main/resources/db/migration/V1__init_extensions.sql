-- 平台配置库固定使用 PostgreSQL，并预留 PostGIS 空间能力。
-- 若数据库镜像未安装 PostGIS，CREATE EXTENSION postgis 会明确失败。
-- 本地开发请使用 database/compose.yaml 中的 postgis/postgis 镜像。

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_available_extensions
        WHERE name = 'postgis'
    ) THEN
        RAISE EXCEPTION
            'PostGIS extension is not available. Install PostGIS for this PostgreSQL server or use database/compose.yaml with the postgis/postgis image.';
    END IF;
END;
$$;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;
