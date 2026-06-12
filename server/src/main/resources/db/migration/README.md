# Flyway 迁移目录

本目录用于保存平台配置库的 Flyway 迁移脚本。

- 第一阶段第 2 步已建立 PostgreSQL/PostGIS 扩展和第一批平台配置核心表。
- Flyway 默认启用，可通过 `FLYWAY_ENABLED=false` 临时关闭。
- 平台数据库固定使用 PostgreSQL，空间能力预留 PostgreSQL + PostGIS。
- 平台配置表统一放在 `platform` schema，不绑定外部水务业务表。
- 后续脚本必须继续使用 Flyway 版本命名规则，不得修改已经执行过的迁移脚本。
