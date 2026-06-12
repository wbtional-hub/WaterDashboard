# CODEX_TASK_LOG.md

## 第 0 次开发初始化

- 当前为第 0 次开发初始化。
- 本次目标是建立 AI 开发记忆体机制。
- 本次不开发业务功能。
- 本次新增 `AGENTS.md`、`AI_MEMORY.md`、`AI_DEVELOPMENT_RULES.md`、`CODEX_TASK_LOG.md`、`CODEX_NEXT_TASK.md`。
- 本次整理 `docs/` 目录，复制 V17 主设计文档，并生成可读取的设计文档摘要。
- 后续开发必须先读取这些文件。

## 初始化结果

- 已将 V17 主设计文档纳入 `docs/` 目录管理。
- 已固化系统定位、固定技术栈、权限要求、安全要求、AI 边界、知识产权保护要求和 CODEX 开发治理要求。
- 本次未创建前端代码、后端代码、数据库表或业务功能。

## 第 1 次方案固化与开发入口更新

- 已完成第一阶段最小闭环开发方案设计，并保存为 `docs/ADR/ADR-001-第一阶段最小闭环开发方案.md`。
- 本次没有编写业务代码，没有创建数据库表，没有开发页面。
- 本次明确了后端模块、前端页面、数据库表、接口、发布流程、Dashboard Context Manifest、`traceId`、权限与 License 预留点。
- 已给出基础框架推荐方案：后端优先 Java Spring Boot，前端采用 Vue 3 + Vite + TypeScript + Pinia + Vue Router，数据库采用 PostgreSQL + PostGIS。

## 当前风险

- 前后端基础框架还未最终确认。
- SQL 安全不能只靠关键字拦截，必须结合 SQL 解析器、只读账号、超时和限行。
- 密钥、连接串、Token 不得返回前端。
- 草稿、发布版本、组件配置 JSON 必须版本化。

## 下一步要求

- 下一步进入第一阶段编码前，必须先确认基础工程选型。

## 第 2 次开发：第一阶段第 1 步独立启动工程骨架

### 本次目标

- 建立“统一智慧水务大屏系统”的独立微服务工程骨架。
- 本次只开发工程基础能力，不开发完整业务功能。

### 实际修改

- 已建立 Java 17 + Spring Boot + Maven 后端工程。
- 已建立 Vue 3 + Vite + TypeScript + Pinia + Vue Router 前端工程。
- 已预留 PostgreSQL JDBC、PostGIS Docker Compose 和 Flyway 迁移目录。
- 已建立统一响应结构：`success`、`code`、`message`、`data`、`traceId`。
- 已建立请求 `traceId` 机制，支持沿用 `X-Trace-Id`、响应头返回、响应体返回和日志输出。
- 已建立全局异常处理，不向前端暴露完整堆栈。
- 已建立 `GET /api/health` 健康检查接口和骨架异常验证接口。
- 已建立前端 `/`、`/health` 路由、基础布局和后端健康检查调用。
- 已建立前端统一 request 封装，可读取后端 `traceId` 并显示友好错误摘要。

### 验证结果

- `mvn test` 通过，3 个后端接口测试全部成功。
- `mvn package -DskipTests` 通过，后端可生成可执行 JAR。
- 后端实际启动成功，`GET /api/health` 返回统一响应，响应头和响应体均包含 `traceId`。
- `GET /api/health/error-demo` 返回统一错误结构，不暴露堆栈。
- `npm install` 成功，未发现依赖漏洞。
- `npm run build` 成功。
- `npm run dev` 实际启动成功，首页返回 HTTP 200，Vite 代理可调用后端健康检查接口。
- 当前环境的浏览器自动化运行库缺少 `playwright-core`，未完成真实浏览器 DOM 验证；前端错误处理逻辑和后端不可用时的代理失败已完成结构验证。

### 遗留风险

- Flyway 默认关闭，下一步建立平台配置库迁移基线后再启用。
- 数据库真实密码必须通过环境变量或本地 `.env` 提供，不得提交仓库。
- 当前 CORS 仅允许本地开发地址，后续需按部署环境配置。
- `/api/health/error-demo` 仅用于骨架异常验证，后续应移除或限制在非生产环境。

### 未开发能力

- 未开发数据源配置中心、组件模板管理、大屏编辑器、SQL 执行、AI Gateway、GIS、三维、G6、完整权限中心和 License Center。

## 第 3 次开发：第一阶段第 2 步 PostgreSQL/PostGIS 平台配置库与 Flyway 基线

### 本次目标

- 建立 PostgreSQL/PostGIS 平台配置库基础能力和 Flyway 迁移基线。
- 本次只开发平台配置库、数据库健康检查和相关文档，不开发完整业务页面或 SQL 执行功能。

### 实际修改

- 已完善 Spring Boot PostgreSQL JDBC 连接池配置，数据库密码继续通过 `PLATFORM_DB_PASSWORD` 环境变量读取。
- 已默认启用 Flyway，并配置 `platform` schema、迁移校验和 UTF-8 编码。
- 已新增 `V1__init_extensions.sql`，用于启用 `uuid-ossp` 和 `postgis`，并在 PostGIS 不可用时给出清晰错误说明。
- 已新增 `V2__init_platform_tables.sql`，创建 12 张平台配置核心表、必要索引、外键和不可变版本快照触发器。
- `data_source.config_json` 已增加常见明文密钥键约束，`secret_ref` 仅保存密钥引用。
- 已新增数据库健康检查服务，`GET /api/health` 返回数据库状态、PostgreSQL 连接状态和 PostGIS 可用状态。
- 数据库异常不会导致健康接口崩溃，也不会向前端返回连接串、用户名、密码或密钥。
- 已更新前端健康页，展示 PostgreSQL 与 PostGIS 状态。
- 已完善 `database/compose.yaml`、`.env.example`、`README.md` 和迁移目录说明。

### 验证结果

- `mvn test` 通过，3 个后端接口测试全部成功。
- `mvn package -DskipTests` 通过。
- `npm run build` 通过。
- Docker Compose 配置解析通过，但本机 Docker Desktop Linux Engine 未运行，容器启动验证未完成。
- 已连接用户提供的本地 PostgreSQL 18.3 实例，验证 PostgreSQL 连接成功。
- 本地 PostgreSQL 18.3 未安装 PostGIS，Flyway `V1` 按设计明确失败，并提示安装 PostGIS 或使用 `postgis/postgis` 镜像。
- `V2` 已在 PostgreSQL 18.3 上完成事务内执行并回滚，12 张平台表、索引、外键和触发器通过 SQL 语法验证，未绕过 Flyway 持久化落库。
- 在 `FLYWAY_ENABLED=false` 的降级验证中，后端启动成功，`GET /api/health` 返回 `postgresConnected=true`、`postgisAvailable=false`、`databaseStatus=DEGRADED`，响应保持统一结构和 `traceId`。
- 前端 `/health` 页面和 Vite 代理健康接口返回 HTTP 200。
- 仓库中未写入或提交真实数据库密码。

### 遗留风险

- 必须在安装 PostGIS 的 PostgreSQL 实例或可运行的 PostGIS Docker 容器上重新执行 Flyway，才能完成 `V1`、`V2` 持久化迁移验证。
- 当前本机 PostgreSQL 18.3 只提供 `uuid-ossp`，不满足 V17 固定的 PostgreSQL + PostGIS 技术栈。
- Docker Desktop 服务在当前环境中无法启动，Compose 容器验证受阻。
- `/api/health/error-demo` 仍仅用于骨架异常验证，后续应移除或限制在非生产环境。

### 未开发能力

- 未开发数据源管理、组件模板管理、大屏编辑器、SQL 预览执行、AI Gateway、GIS、三维、G6、完整权限中心和 License Center。

## 第 4 次开发：第一阶段第 2 步 PostGIS 与 Flyway 持久化迁移验证收尾

### 本次目标

- 在已安装 PostGIS 的本地 PostgreSQL 18.3 环境中完成 Flyway 持久化迁移验证。
- 保持应用运行账号与 Flyway 迁移账号分离，不把真实数据库密码写入仓库。

### 实际修改

- 已补充 Flyway 独立迁移账号配置：`FLYWAY_URL`、`FLYWAY_USER`、`FLYWAY_PASSWORD`。
- 已更新 `database/README.md`，说明应用用户与 Flyway DBA 用户的本地开发配置方式。
- 已新增 `V3__grant_platform_permissions.sql`，为应用用户 `water_dashboard` 授予 `platform` schema、表、序列和函数权限。

### 验证结果

- 已确认本地 PostgreSQL 18.3 当前数据目录为 PostgreSQL 安装目录下的 `pgsql/data`。
- 已确认目标库 `water_dashboard` 和应用用户 `water_dashboard` 存在。
- 已确认目标数据库可用扩展包含 `postgis 3.6.2` 和 `uuid-ossp 1.1`。
- 使用 `FLYWAY_ENABLED=true`、应用用户 `water_dashboard`、Flyway 用户 `postgres` 启动后端成功。
- Flyway 迁移记录已包含：
  - `V1 init extensions`
  - `V2 init platform tables`
  - `V3 grant platform permissions`
- `platform` schema 中 12 张平台核心表已落库。
- `postgis` 和 `uuid-ossp` 扩展已启用。
- 平台索引数量验证为 41。
- `dashboard_version` 不可变快照触发器已存在，覆盖 `UPDATE` 和 `DELETE`。
- 应用用户 `water_dashboard` 已可访问 `platform.dashboard`。
- `GET /api/health` 返回 `databaseStatus=UP`、`postgresConnected=true`、`postgisAvailable=true`，并保持统一响应和 `traceId`。
- `mvn test` 通过，3 个后端测试全部成功。
- `npm run build` 通过。
- 仓库扫描未发现真实数据库密码被写入项目文件。

### 遗留风险

- 当前迁移使用本地 DBA 用户启用 PostGIS；生产部署时应由部署流程提供受控的 Flyway 迁移账号或由 DBA 预先授权。
- `/api/health/error-demo` 仍仅用于骨架异常验证，后续应移除或限制在非生产环境。

### 未开发能力

- 未开发数据源配置中心、组件模板管理、大屏编辑器、SQL 预览执行、AI Gateway、GIS、三维、G6、完整权限中心和 License Center。

## 第 5 次开发：第一阶段第 3 步数据源配置中心基础能力

### 本次目标

- 开发数据源配置中心最小闭环能力。
- 本次只实现 PostgreSQL 数据源的配置、列表、详情、编辑、逻辑停用和连接测试。
- 本次不开发卡片 SQL 预览、大屏编辑器、AI Gateway、GIS、三维、完整权限中心或 License Center。

### 实际修改

- 新增后端数据源模块 `datasource`，提供 `GET/POST/PUT/PATCH/POST test` 等基础接口。
- 新增 `DataSourceSecretService`，将数据源密码封装为后端受控加密引用，预留后续 KMS/密钥中心替换入口。
- 新增 `DataSourceConnectionTester`，对 PostgreSQL 数据源执行带超时的连接测试，并返回脱敏结果。
- 新增数据源新增、修改、停用审计写入 `platform.audit_log`。
- 新增连接测试健康日志写入 `platform.data_source_health_log`。
- 新增 `V4__allow_temp_data_source_health_log.sql`，允许未保存数据源的临时连接测试记录落库，并增加脱敏测试目标摘要字段。
- 新增前端 `/data-sources` 数据源配置中心页面，支持列表筛选、新增、编辑、测试连接和停用。
- 前端密码框编辑时不回显原密码，响应和页面不展示密码、`secret_ref`、完整 JDBC URL 或完整异常堆栈。
- `.gitignore` 已加入 `sn.txt`，避免本地密码文件继续进入版本管理。

### 新增接口

- `GET /api/platform/data-sources`
- `GET /api/platform/data-sources/{id}`
- `POST /api/platform/data-sources`
- `PUT /api/platform/data-sources/{id}`
- `PATCH /api/platform/data-sources/{id}/disable`
- `POST /api/platform/data-sources/{id}/test`
- `POST /api/platform/data-sources/test-temp`

### 验证结果

- `mvn test` 通过，3 个后端测试全部成功。
- `mvn package -DskipTests` 通过。
- `npm run build` 通过。
- 后端使用本地 `water_dashboard` 库启动成功，Flyway 已执行到 `V4`。
- `GET /api/health` 返回 `databaseStatus=UP`、`postgresConnected=true`、`postgisAvailable=true`。
- `GET /api/platform/data-sources` 返回统一响应并带 `traceId`。
- 已新增一个 PostgreSQL 测试数据源，保存后连接测试成功。
- 已完成数据源修改，空密码保持原密钥引用。
- 已完成数据源逻辑停用，状态为 `DISABLED`。
- 已完成未保存临时参数连接测试，健康日志可落库。
- `platform.data_source_health_log` 已写入保存后连接测试记录和临时连接测试记录。
- `platform.audit_log` 已写入数据源新增、修改、停用记录。
- 前端 `/data-sources` 返回 HTTP 200，Vite 代理可访问数据源列表接口。
- 接口响应扫描未发现密码、`secret_ref/secretRef`、完整连接串等敏感字段。
- 仓库和运行日志扫描未发现真实数据库密码被写入项目文件或日志。

### 遗留风险

- 当前 `DataSourceSecretService` 是本地 AES-GCM 封装，适合第一阶段开发；生产部署应替换为 KMS、Vault 或受控密钥中心。
- 当前操作人暂用 `system`，后续权限中心建立后应接入真实 `UserContext`。
- 连接测试只验证连通性，不执行 SQL 预览，也不代表业务查询权限已通过。
- `/api/health/error-demo` 仍仅用于骨架异常验证，后续应移除或限制在非生产环境。
- 仓库根目录存在过 `sn.txt` 明文密码文件，已加入 `.gitignore`；建议移出项目目录或删除。

### 未开发能力

- 未开发组件模板管理、大屏编辑器、卡片 SQL 预览执行、字段映射、发布体检、AI Gateway、GIS、三维、G6、完整权限中心和 License Center。
