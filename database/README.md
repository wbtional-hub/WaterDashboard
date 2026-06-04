# 数据库骨架说明

本目录用于管理“统一智慧水务大屏系统”的平台数据库运行配置和数据库级说明。

## 固定技术路线

- 平台数据库：PostgreSQL
- 空间数据库：PostgreSQL + PostGIS
- 数据库迁移：Flyway

## 当前阶段

第一阶段第 1 步只预留数据库连接和迁移目录，不创建业务表。

## 启动 PostGIS

1. 在本目录创建 `.env`，设置 `POSTGRES_PASSWORD`。
2. 执行：

```bash
docker compose up -d
```

3. 后端启用 Flyway 前，设置 `FLYWAY_ENABLED=true`。

任何真实密码、Token、连接串或密钥都不得提交到仓库。

