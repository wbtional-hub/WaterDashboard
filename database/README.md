# PostgreSQL/PostGIS 平台配置库

本目录用于管理“统一智慧水务大屏系统”的本地 PostgreSQL/PostGIS 运行配置和数据库说明。

## 固定技术路线

- 平台数据库：PostgreSQL
- 空间数据库：PostgreSQL + PostGIS
- 数据库迁移：Flyway
- 平台配置表 schema：`platform`

平台配置库只保存大屏平台自身配置、版本、日志和审计信息，不绑定任何外部水务业务系统的数据结构。

## 本地启动数据库

1. 在 `database/` 目录复制 `.env.example` 为本地 `.env`。
2. 修改 `.env` 中的 `POSTGRES_PASSWORD` 和 `PLATFORM_DB_PASSWORD`，两者应保持一致。
3. `.env` 只用于本地环境，不得提交到仓库。
4. 启动 PostgreSQL/PostGIS：

```powershell
cd D:\20.develop64\智慧水务大屏\database
docker compose up -d
docker compose ps
```

`postgis/postgis:16-3.4` 镜像已包含 PostGIS。容器启动时会创建：

- 数据库：`water_dashboard`
- 用户：`water_dashboard`
- 端口：`5432`

这些值可通过本地 `.env` 覆盖。

## 启用 UUID 与 PostGIS

后端启动时 Flyway 会执行：

- `V1__init_extensions.sql`：启用 `uuid-ossp` 与 `postgis` 扩展。
- `V2__init_platform_tables.sql`：创建 `platform` schema 和第一批平台配置核心表。

若目标 PostgreSQL 环境未安装 PostGIS，`CREATE EXTENSION postgis` 会失败。请先安装 PostGIS，或使用本目录提供的 PostGIS Docker 镜像。执行扩展迁移的数据库账号需要具备创建扩展的权限。

## 后端连接数据库

在启动后端前设置环境变量：

```powershell
$env:PLATFORM_DB_URL='jdbc:postgresql://localhost:5432/water_dashboard'
$env:PLATFORM_DB_USERNAME='water_dashboard'
$env:PLATFORM_DB_PASSWORD='你的本地密码'
$env:FLYWAY_ENABLED='true'

cd D:\20.develop64\智慧水务大屏\server
mvn spring-boot:run
```

后端不会向前端返回数据库 URL、用户名、密码、Token、连接串或密钥。

## Flyway 迁移账号

PostGIS 在部分 PostgreSQL 发行版中需要超级用户或 DBA 权限才能执行 `CREATE EXTENSION postgis`。推荐做法：

- 应用运行连接使用普通用户，例如 `water_dashboard`。
- Flyway 初始化迁移使用 DBA 用户，例如本地开发中的 `postgres`。
- 两类密码都通过环境变量传入，不写入仓库。

示例：

```powershell
$env:PLATFORM_DB_URL='jdbc:postgresql://localhost:5432/water_dashboard'
$env:PLATFORM_DB_USERNAME='water_dashboard'
$env:PLATFORM_DB_PASSWORD='你的应用用户密码'
$env:FLYWAY_ENABLED='true'
$env:FLYWAY_URL=$env:PLATFORM_DB_URL
$env:FLYWAY_USER='postgres'
$env:FLYWAY_PASSWORD='你的本地 DBA 密码'
```

## 验证

查看容器状态：

```powershell
docker compose ps
```

查看扩展：

```powershell
docker compose exec postgis psql -U water_dashboard -d water_dashboard -c "\dx"
```

查看 Flyway 迁移记录和平台表：

```powershell
docker compose exec postgis psql -U water_dashboard -d water_dashboard -c "select installed_rank, version, description, success from platform.flyway_schema_history order by installed_rank;"
docker compose exec postgis psql -U water_dashboard -d water_dashboard -c "\dt platform.*"
```

停止数据库：

```powershell
docker compose down
```

如需同时删除本地数据库卷，必须先确认数据不再需要，再执行 `docker compose down -v`。
