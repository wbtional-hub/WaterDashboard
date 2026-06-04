# Flyway 迁移目录

本目录用于保存平台配置库的 Flyway 迁移脚本。

- 第一阶段第 1 步仅建立迁移目录，不创建业务表。
- 启用迁移前设置 `FLYWAY_ENABLED=true`。
- 平台数据库固定使用 PostgreSQL，空间能力预留 PostgreSQL + PostGIS。
- 后续脚本应使用 Flyway 命名规则，例如 `V1__init_platform_schema.sql`。

