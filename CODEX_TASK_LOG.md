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

## 第 6 次开发：第一阶段第 4 步组件模板管理基础能力

### 本次目标

- 开发组件模板管理最小闭环能力。
- 本次只实现组件模板元数据、启停、版本契约和前端管理页面。
- 本次不开发大屏编辑器、拖拽画布、卡片 SQL 预览、真实 OpenLayers 地图卡、真实 Babylon.js 三维卡、真实 G6 拓扑卡、AI Gateway、完整权限中心或 License Center。

### 实际修改

- 新增后端组件模板模块 `componenttemplate`。
- 新增组件模板基础接口，支持列表、详情、新增、编辑、启用、停用。
- 新增组件模板版本接口，支持创建版本、版本列表和版本详情。
- 支持 `dataContractJson`、`defaultConfigJson`、`fieldMappingSchemaJson` 三类 JSON 契约。
- 支持渲染引擎类型：`TEXT_CARD`、`ECHARTS_LINE`、`ECHARTS_BAR`、`TABLE_LIST`、`OPENLAYERS_MAP`、`BABYLON_SCENE`、`G6_TOPOLOGY`。
- OpenLayers、Babylon.js、G6 当前只做模板类型预留，没有引入真实渲染能力。
- 支持 `licenseScope`、`signature`、`checksum` 预留字段。
- 新增 `V5__extend_component_template_metadata.sql`，为 `platform.component_template` 补充 `description`、最小尺寸、默认尺寸和渲染引擎索引。
- 新增前端 `/component-templates` 页面，支持列表筛选、新增、编辑、启用、停用、创建版本、JSON 格式化和基础校验。
- 所有新增、修改、启用、停用、创建版本操作写入 `platform.audit_log`。
- 所有接口继续返回 `success`、`code`、`message`、`data`、`traceId`。

### 新增接口

- `GET /api/platform/component-templates`
- `GET /api/platform/component-templates/{id}`
- `POST /api/platform/component-templates`
- `PUT /api/platform/component-templates/{id}`
- `PATCH /api/platform/component-templates/{id}/enable`
- `PATCH /api/platform/component-templates/{id}/disable`
- `POST /api/platform/component-templates/{id}/versions`
- `GET /api/platform/component-templates/{id}/versions`
- `GET /api/platform/component-templates/{id}/versions/{versionId}`

### 验证结果

- `mvn test` 通过，3 个后端测试全部成功。
- 首次执行 `mvn package -DskipTests` 时因旧后端 JAR 正在运行被 Windows 锁定而失败；停止旧后端进程后重新执行成功。
- `mvn package -DskipTests` 最终通过。
- `npm run build` 通过。
- 后端使用本地 `water_dashboard` 库启动成功，Flyway 已执行到 `V5`。
- `GET /api/health` 返回 `databaseStatus=UP`、`postgresConnected=true`、`postgisAvailable=true`。
- `GET /api/platform/data-sources` 仍可正常访问，数据源配置中心未被破坏。
- 前端 `/data-sources` 返回 HTTP 200。
- 前端 `/component-templates` 返回 HTTP 200。
- 已验证组件模板新增、编辑、启用、停用、创建版本、查询详情和查询版本详情。
- 已验证 `platform.component_template_version` 有版本记录。
- 已验证 `platform.audit_log` 包含组件模板新增、修改、启用、停用、创建版本记录。
- 接口响应包含 `traceId`，未返回完整异常堆栈。

### 遗留风险

- 当前操作人仍暂用 `system`，后续权限中心建立后应接入真实 `UserContext`。
- JSON 契约当前做基础合法性和危险脚本片段校验，后续需要按模板类型引入更严格的 JSON Schema 校验。
- 当前组件模板只管理元数据和契约，不包含真实运行时渲染器。
- License、签名、校验和仅预留字段，尚未接入 License Center 和组件签名校验。
- `/api/health/error-demo` 仍仅用于骨架异常验证，后续应移除或限制在非生产环境。

### 未开发能力

- 未开发大屏编辑器、拖拽画布、卡片 SQL 预览执行、字段映射、发布体检、浏览页运行时、AI Gateway、GIS、三维、G6、完整权限中心和 License Center。

## 第 7 次开发：第一阶段第 5 步大屏管理与草稿基础能力

### 本次目标

- 开发大屏管理与空草稿基础保存能力。
- 本次只实现大屏基础信息管理、空草稿自动创建、草稿读取和草稿基础配置保存。
- 本次不开发拖拽画布、卡片实例新增、卡片 SQL 预览、发布版本、浏览态大屏、真实 GIS/三维/G6、AI Gateway、完整权限中心或 License Center。

### 实际修改

- 新增后端大屏模块 `dashboard`。
- 新增大屏基础接口，支持列表、详情、新增、编辑、启用、停用。
- 新建大屏时自动创建 `dashboard_draft` 空草稿，初始 `revision=1`。
- 新增草稿读取和保存接口，保存草稿时只更新 `platform.dashboard_draft`，并递增 `revision`。
- 草稿 `configJson` 采用可扩展结构，包含 `schemaVersion`、`canvas`、`theme`、`cards`、`interactions`、`aiContext`。
- `cards` 当前保持空数组，预留后续卡片实例配置。
- `aiContext` 当前只预留 Dashboard Context Manifest，不调用 AI。
- 新增 `V6__extend_dashboard_metadata.sql`，为 `platform.dashboard` 补充 `description`、屏幕尺寸、背景配置、主题配置和编码索引。
- 新增前端 `/dashboards` 页面，支持大屏列表筛选、新增、编辑、启用、停用、查看草稿、生成基础草稿、格式化 JSON 和保存草稿。
- 所有新增、修改、启用、停用、初始化草稿、保存草稿操作写入 `platform.audit_log`。
- 所有接口继续返回 `success`、`code`、`message`、`data`、`traceId`。

### 新增接口

- `GET /api/platform/dashboards`
- `GET /api/platform/dashboards/{id}`
- `POST /api/platform/dashboards`
- `PUT /api/platform/dashboards/{id}`
- `PATCH /api/platform/dashboards/{id}/enable`
- `PATCH /api/platform/dashboards/{id}/disable`
- `GET /api/platform/dashboards/{id}/draft`
- `PUT /api/platform/dashboards/{id}/draft`

### 验证结果

- `mvn test` 通过，3 个后端测试全部成功。
- `mvn package -DskipTests` 通过。
- `npm run build` 通过。
- 后端使用本地 `water_dashboard` 库启动成功，Flyway 已执行到 `V6`。
- `GET /api/health` 返回 `databaseStatus=UP`、`postgresConnected=true`、`postgisAvailable=true`。
- 前端 `/data-sources` 返回 HTTP 200，数据源配置中心未被破坏。
- 前端 `/component-templates` 返回 HTTP 200，组件模板管理未被破坏。
- 前端 `/dashboards` 返回 HTTP 200。
- 已验证大屏新增、编辑、启用、停用。
- 已验证新建大屏自动创建空草稿，初始 `revision=1`。
- 已验证草稿保存成功，保存后 `revision=2`。
- 已验证草稿 `cards` 为可扩展空数组。
- 已验证 `platform.dashboard_version` 未写入记录，草稿保存未影响发布版本设计。
- 已验证 `platform.audit_log` 包含大屏新增、修改、启用、停用、草稿初始化、草稿保存记录。
- 接口响应包含 `traceId`，未返回完整异常堆栈。

### 遗留风险

- 当前操作人仍暂用 `system`，后续权限中心建立后应接入真实 `UserContext`。
- 草稿 JSON 当前做基础结构和危险脚本片段校验，后续进入编辑器阶段需要更严格的布局、卡片实例和字段映射 Schema 校验。
- 当前草稿仅保存空画布配置，不包含拖拽画布、卡片实例、SQL 绑定、发布体检或浏览态运行能力。
- `/api/health/error-demo` 仍仅用于骨架异常验证，后续应移除或限制在非生产环境。

### 未开发能力

- 未开发拖拽画布、卡片实例新增、卡片 SQL 预览执行、字段映射、发布体检、不可变版本发布、浏览页运行时、AI Gateway、GIS、三维、G6、完整权限中心和 License Center。

## 第 8 次开发：第一阶段第 6 步大屏编辑器空画布与组件模板库入口

### 本次目标

- 开发大屏编辑器基础骨架。
- 本次只实现编辑器页面、草稿加载、空画布展示、启用组件模板库入口、基础属性面板和草稿保存。
- 本次不开发真实拖拽、卡片实例新增、卡片 SQL 预览、发布版本、浏览态大屏、真实 GIS/三维/G6、AI Gateway、完整权限中心或 License Center。

### 实际修改

- 新增前端 `/dashboards/:id/editor` 编辑器页面。
- 大屏管理页面新增“进入编辑器”入口。
- 编辑器加载大屏草稿，并根据 `draft.configJson.canvas.width`、`height`、`background` 展示空画布。
- 编辑器左侧加载已启用组件模板列表，展示模板名称、分类和渲染引擎。
- 编辑器中间展示空画布、网格背景、画布尺寸、卡片数量和空状态提示。
- 编辑器右侧提供画布宽度、高度、背景色、主题色和 AI Context 预留开关。
- 编辑器保存草稿时继续复用 `PUT /api/platform/dashboards/{id}/draft`，只更新 `dashboard_draft`。
- 顶部预留“预览”“发布”按钮，但置为不可用并显示后续阶段开放。
- 本次没有新增后端接口，没有新增数据库迁移脚本。

### 复用接口

- `GET /api/platform/dashboards`
- `GET /api/platform/dashboards/{id}/draft`
- `PUT /api/platform/dashboards/{id}/draft`
- `GET /api/platform/component-templates?status=ENABLED`

### 验证结果

- `mvn test` 通过，3 个后端测试全部成功。
- `mvn package -DskipTests` 通过。
- `npm run build` 通过。
- 后端使用本地 `water_dashboard` 库启动成功，Flyway 当前保持 `V6`，没有新增迁移。
- `GET /api/health` 返回 `databaseStatus=UP`、`postgresConnected=true`、`postgisAvailable=true`。
- 前端 `/data-sources` 返回 HTTP 200，数据源配置中心未被破坏。
- 前端 `/component-templates` 返回 HTTP 200，组件模板管理未被破坏。
- 前端 `/dashboards` 返回 HTTP 200，大屏管理未被破坏。
- 前端 `/dashboards/{id}/editor` 返回 HTTP 200。
- 已验证编辑器依赖接口可加载大屏草稿。
- 已验证左侧可读取已启用组件模板列表。
- 已验证保存草稿后 `revision` 从 2 递增到 3。
- 已验证保存后画布宽度、高度、背景色、主题色更新成功。
- 已验证 `cards` 仍为空数组，没有创建卡片实例。
- 已验证 `platform.dashboard_version` 未写入记录，草稿保存未影响发布版本设计。
- 已验证 `platform.audit_log` 包含 `DASHBOARD_DRAFT_SAVE` 记录。
- 接口响应包含 `traceId`，未返回完整异常堆栈。

### 遗留风险

- 编辑器当前只展示空画布和模板库入口，尚未实现真实拖拽、卡片实例创建、布局保存和属性编辑。
- 组件模板库当前只做列表展示，不执行模板放置和运行时渲染。
- AI Context 当前只是草稿字段预留，不调用 AI Gateway，不生成发布 Manifest。
- 当前操作人仍暂用 `system`，后续权限中心建立后应接入真实 `UserContext`。
- `/api/health/error-demo` 仍仅用于骨架异常验证，后续应移除或限制在非生产环境。

### 未开发能力

- 未开发真实拖拽、卡片实例新增、卡片 SQL 预览执行、字段映射、发布体检、不可变版本发布、浏览页运行时、AI Gateway、GIS、三维、G6、完整权限中心和 License Center。

## 第 9 次开发：第一阶段第 7 步卡片实例基础添加与画布草稿保存

### 本次目标

- 开发编辑器中从组件模板库添加基础卡片实例的最小闭环。
- 本次只实现卡片实例添加、基础属性编辑、删除、草稿 `cards` 同步保存和审计记录。
- 本次不开发真实拖拽、卡片 SQL 预览、字段映射、发布版本、浏览态大屏、真实 GIS/三维/G6、AI Gateway、完整权限中心或 License Center。
- 本次同时新增 `V7` 迁移，修复 DbVisualizer 等工具对 PostgreSQL `?|` 操作符的兼容性问题。

### 实际修改

- 新增后端卡片实例 DTO：`DashboardCardRequest`、`DashboardCardResponse`。
- 新增卡片实例接口：列表、添加、编辑、删除。
- 新增 `dashboard_card` 与 `dashboard_draft.config_json.cards` 的同步逻辑。
- 卡片实例保存基础字段：模板引用、标题、位置、尺寸、启用状态、AI 启用预留开关、渲染引擎和默认数据绑定占位。
- 编辑器左侧组件模板库新增“添加到画布”入口。
- 编辑器画布新增卡片占位展示和选中状态。
- 编辑器右侧属性面板支持编辑卡片标题、位置、尺寸、启用状态和 AI 启用预留开关。
- 编辑器支持删除草稿卡片实例。
- 草稿保存继续只影响 `dashboard_draft`，未写入 `dashboard_version`。
- 新增 JSON 配置敏感字段名拦截，禁止在草稿或卡片配置中写入 `password`、`token`、`secret`、连接串、私钥等字段。
- 新增 `V7__normalize_data_source_secret_check.sql`，将 `config_json ?| ARRAY[...]` 约束替换为 `jsonb_exists_any(config_json, ARRAY[...]::text[])`。
- 更新 `database/README.md`，补充 DbVisualizer、`?|` 操作符和 `DO $$` 手工执行注意事项。

### 新增接口

- `GET /api/platform/dashboards/{id}/draft/cards`
- `POST /api/platform/dashboards/{id}/draft/cards`
- `PUT /api/platform/dashboards/{id}/draft/cards/{cardId}`
- `DELETE /api/platform/dashboards/{id}/draft/cards/{cardId}`

### 验证结果

- `mvn test` 通过，3 个后端测试全部成功。
- `mvn package -DskipTests` 通过。
- `npm run build` 通过。
- 后端新版服务启动成功，`GET /api/health` 返回 `databaseStatus=UP`、`postgresConnected=true`、`postgisAvailable=true`。
- Flyway 已确认 `V7` 迁移落库，`platform.flyway_schema_history` 中 `version=7` 记录数为 1。
- 前端 `/`、`/data-sources`、`/component-templates`、`/dashboards`、`/dashboards/{id}/editor` 均返回 HTTP 200。
- 已验证添加卡片实例成功，接口响应包含 `traceId`，未返回密码、真实连接串或 JDBC URL。
- 已验证编辑卡片标题、位置、尺寸和 AI 启用预留开关成功。
- 已验证删除卡片实例成功，删除后 `dashboard_card` 无 Step7 临时卡片残留。
- 已验证删除后 `dashboard_draft.config_json.cards` 为空数组。
- 已验证 `dashboard_version` 未写入记录，卡片草稿操作未影响发布版本。
- 已验证 `platform.audit_log` 包含 `DASHBOARD_CARD_CREATE`、`DASHBOARD_CARD_UPDATE`、`DASHBOARD_CARD_DELETE` 记录。
- 已验证向卡片配置写入敏感字段会被拒绝，且未落库。

### 遗留风险

- 当前卡片放置仍为按钮添加和属性面板编辑，不是真实拖拽、缩放、吸附线或图层管理。
- 卡片当前只保存配置和占位展示，不执行 SQL、不绑定数据源、不做字段映射。
- 卡片数据绑定对象当前只是占位结构，后续进入 SQL 配置前必须先补齐 SQL 解析、只读账号、超时、限行、审计和脱敏诊断策略。
- 当前操作人仍暂用 `system`，后续权限中心建立后应接入真实 `UserContext`。
- `/api/health/error-demo` 仍仅用于骨架异常验证，后续应移除或限制在非生产环境。

### 未开发能力

- 未开发真实拖拽画布、卡片 SQL 预览执行、字段映射、发布体检、不可变版本发布、浏览页运行时、AI Gateway、GIS、三维、G6、完整权限中心和 License Center。

## 第 10 次工作：数据库迁移脚本检查与 V7 执行验证

### 本次目标

- 本次不开发新功能，只检查并验证 `V7__normalize_data_source_secret_check.sql`。
- 确认 `V7` 修复 DbVisualizer 对 PostgreSQL JSONB `?|` 操作符的参数误识别问题。
- 确认 `V1` 至 `V6` 历史迁移脚本不被修改，避免 Flyway checksum 不一致。

### 检查结论

- `V7` 已使用 `jsonb_exists_any(config_json, ARRAY[...]::text[])` 替代 `config_json ?| ARRAY[...]`。
- `V7` 不包含真实密码、Token、连接串或密钥。
- `V7` 保留并强化了 `ck_data_source_config_no_plain_secret` 的安全意图，继续禁止 `data_source.config_json` 保存 `password`、`token`、`secret`、`connectionString`、`connection_string`、`privateKey`、`private_key` 等敏感字段。
- 未修改已执行成功的 `V1` 至 `V6` 历史迁移脚本。

### 执行与数据库验证

- 通过 Spring Boot 启动触发 Flyway 校验，日志显示 `Successfully validated 8 migrations`。
- Flyway 日志显示当前 `platform` schema 版本为 `7`，并提示 `Schema "platform" is up to date. No migration necessary.`。
- `platform.flyway_schema_history` 查询确认 `V1` 至 `V7` 均为 `success=true`。
- `platform.data_source` 查询确认约束 `ck_data_source_config_no_plain_secret` 存在。
- 约束定义已为 `CHECK ((NOT jsonb_exists_any(config_json, ARRAY[...])))`。
- 执行敏感字段负向测试：向 `platform.data_source.config_json` 插入 `{"password":"123456"}` 被检查约束拒绝。
- 负向测试后确认 `code='test_secret_check'` 的测试数据未残留。
- 查询确认已有数据源、组件模板和大屏草稿数据仍存在，未受影响。

### 回归验证

- `mvn test` 通过，3 个后端测试全部成功。
- `mvn package -DskipTests` 通过。
- `npm run build` 通过。
- 后端启动后 `GET /api/health` 返回 `databaseStatus=UP`、`postgresConnected=true`、`postgisAvailable=true`。

### 遗留风险

- 当前 Flyway 版本提示 PostgreSQL 18.3 新于其已测试支持版本，后续生产环境建议评估 Flyway 版本升级。
- 本次负向测试使用数据库约束验证安全底线；后续 SQL 配置和数据绑定仍必须继续做应用层敏感字段校验、脱敏日志和审计。

### 下一步建议

- 第一阶段第 7 步卡片实例基础添加已完成并验证。
- 下一步可进入第一阶段第 8 步：卡片数据源选择与 SQL 配置骨架；该步骤仍不得执行 SQL，不得开发 SQL 预览。

## 第 11 次开发：第一阶段第 8 步卡片数据源选择与 SQL 配置骨架

### 本次目标

- 在大屏编辑器中为已选中卡片增加数据配置和刷新配置。
- 本次只保存卡片数据绑定配置，不执行 SQL，不做 SQL 预览，不做字段自动识别，不做数据查询。
- 保存范围仅限 `dashboard_card.config_json` 和 `dashboard_draft.config_json.cards`，不写入 `dashboard_version`。

### 实际修改

- 后端复用 `PUT /api/platform/dashboards/{id}/draft/cards/{cardId}`，小范围增强卡片配置保存能力。
- 卡片配置支持 `dataBinding.enabled`、`dataBinding.dataSourceId`、`dataBinding.queryType`、`dataBinding.sql`、`dataBinding.params`、`dataBinding.fieldMapping`。
- 卡片配置支持 `refresh.enabled` 和 `refresh.intervalSeconds`。
- 后端增加配置级 SQL 基础校验：非空 SQL 必须以 `SELECT` 或 `WITH` 开头，禁止分号多语句，禁止 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`TRUNCATE`、`CREATE`、`GRANT`、`REVOKE`、`EXECUTE`、`CALL`。
- 后端保存时校验数据源必须存在且状态为 `ENABLED`。
- 后端继续拦截草稿或卡片 JSON 中的密码、Token、连接串、密钥等敏感字段名。
- 前端编辑器加载数据源列表，并在选中卡片后展示基础配置、布局配置、数据配置、刷新配置分组。
- 数据源列表响应补充返回非敏感字段 `code` 和 `environment`，便于编辑器展示名称、编码、类型、环境、状态。
- 前端数据源下拉框只提供已启用数据源，不展示 password、secretRef、JDBC URL、Token 或连接串。
- 前端支持编辑字段映射 JSON 预留对象，但不做自动识别。
- 前端 SQL 预览按钮仅占位禁用，显示下一阶段开放。

### 接口变化

- 未新增接口。
- 调整既有接口：`PUT /api/platform/dashboards/{id}/draft/cards/{cardId}` 支持保存 `dataBinding`、`fieldMapping` 和 `refresh` 配置。

### 验证结果

- `mvn test` 通过，3 个后端测试全部成功。
- `mvn package -DskipTests` 通过。
- `npm run build` 通过。
- 后端启动成功，`GET /api/health` 返回 `databaseStatus=UP`、`postgresConnected=true`、`postgisAvailable=true`。
- 已验证 `GET /api/platform/data-sources` 返回 `code`、`name`、`type`、`environment`、`status`、`enabled` 等非敏感字段，未返回密码、密钥引用或连接串。
- 前端 `/`、`/data-sources`、`/component-templates`、`/dashboards`、`/dashboards/{id}/editor` 均返回 HTTP 200。
- 已验证选中卡片后可保存已启用数据源和 `SELECT 1 AS value` SQL 配置。
- 已验证 `dataBinding.enabled`、`fieldMapping`、`refresh.enabled`、`refresh.intervalSeconds` 可保存。
- 已验证危险 SQL `DROP TABLE ...` 被拒绝，返回 HTTP 400。
- 已验证多语句 SQL `SELECT 1; SELECT 2` 被拒绝，返回 HTTP 400。
- 已验证 `dashboard_card.config_json` 已更新。
- 已验证 `dashboard_draft.config_json.cards` 中对应卡片同步更新。
- 已验证 `dashboard_draft.revision` 从 11 递增到 12。
- 已验证 `dashboard_version` 未写入记录。
- 已验证 `platform.audit_log` 包含 `DASHBOARD_CARD_UPDATE` 记录。
- 接口响应包含 `traceId`，且验证响应未包含真实密码、`secretRef`、JDBC URL 或连接串。
- 仓库扫描未发现真实数据库密码、Token、密钥或真实连接串。

### 遗留风险

- 本次 SQL 校验只是配置保存阶段的基础红线校验，不能替代后续 SQL 解析器级校验。
- 当前未执行 SQL，因此尚未产生 `card_query_log`，也未验证只读账号、限行、超时和 SQL 哈希审计。
- 字段映射当前只保存 JSON 对象，不做字段自动识别或契约校验。
- 当前操作人仍暂用 `system`，后续权限中心建立后应接入真实 `UserContext`。

### 未开发能力

- 未开发 SQL 预览、数据查询、字段自动识别、发布体检、不可变版本发布、浏览页运行时、真实图表/GIS/三维/G6 渲染、AI Gateway、完整权限中心和 License Center。
## 第 12 次开发：第一阶段第 9 步卡片 SQL 预览执行器基础能力

### 本次目标

- 开发卡片 SQL 预览执行器基础能力。
- 本次只允许对已保存的卡片数据绑定配置进行安全预览，不开发字段自动映射、发布版本、浏览态大屏、真实图表渲染、AI Gateway、GIS、三维、G6、完整权限中心或 License Center。
- SQL 预览必须保留统一响应结构、`traceId`、只读连接、超时、限行、SQL 安全校验、脱敏错误和 `card_query_log` 记录。

### 实际修改

- 新增 `V8__extend_card_query_log_for_preview.sql`，扩展 `platform.card_query_log` 的 `query_type`、`row_count`、`error_message` 字段，并补充 `card_id`、`data_source_id` 索引。
- 后端引入 JSqlParser，新增 SQL 安全服务，对预览 SQL 执行解析级校验和基础红线校验。
- 后端新增 `POST /api/platform/dashboards/{dashboardId}/draft/cards/{cardId}/preview-query`。
- 后端预览执行读取 `dashboard_card.config_json.dataBinding`，只允许启用的数据绑定、启用的数据源和 `SQL` 类型查询。
- SQL 预览使用数据源配置中的受控连接，设置只读连接、5 秒查询超时和最大返回行数，默认 20 行，最大 100 行。
- SQL 预览返回列信息、样例行、行数、耗时、`sqlHash` 和 `traceId`，不返回密码、`secret_ref`、JDBC URL、连接串、完整异常堆栈或密钥。
- SQL 成功、失败、超时、禁用数据绑定、禁用数据源等结果均写入 `platform.card_query_log`。
- 前端 `/dashboards/:id/editor` 启用 SQL 预览按钮，展示 loading、预览表格、列名、样例行、耗时、行数和 `traceId`，切换卡片时清空旧预览结果。
- 前端错误提示继续使用友好摘要和 `traceId`，不展示完整 SQL、连接串或堆栈。

### 新增接口

- `POST /api/platform/dashboards/{dashboardId}/draft/cards/{cardId}/preview-query`

### 验证结果

- `mvn test` 通过，3 个后端测试全部成功。
- `mvn package -DskipTests` 通过。
- `npm run build` 通过。
- 后端最新版本在本机备用端口 `18080` 启动成功；当前 `8080` 仍被一个权限较高的旧 Java 进程占用，未强行终止。
- `GET /api/health` 返回 `databaseStatus=UP`。
- Flyway 已执行到 `V8`，`platform.flyway_schema_history` 中 `version=8`、`success=true`。
- 成功预览 `SELECT 1 AS value`，返回 1 列、1 行、`sqlHash` 长度 64，并包含 `traceId`。
- SQL 预览未递增 `dashboard_draft.revision`，验证中 revision 保持 `19 -> 19`。
- 危险 SQL `DROP TABLE ...` 被拒绝，返回 `SQL_FORBIDDEN`。
- 多语句 SQL `SELECT 1; SELECT 2` 被拒绝，返回 `SQL_FORBIDDEN`。
- 超时 SQL 被 5 秒超时机制拒绝，返回 `SQL_TIMEOUT`。
- 未启用数据绑定被拒绝，返回 `DATA_BINDING_DISABLED`。
- 停用数据源被拒绝，返回 `DATA_SOURCE_DISABLED`。
- `platform.card_query_log` 已记录 `SUCCESS`、`FORBIDDEN`、`TIMEOUT` 等预览结果。
- `platform.dashboard_version` 未写入记录。
- `dashboard_card.config_json` 未持久化预览结果列、行、耗时或行数，仅保留配置。
- 前端 `/`、`/data-sources`、`/component-templates`、`/dashboards`、`/dashboards/{id}/editor` 均返回 HTTP 200。
- 仓库扫描未发现真实数据库密码、Token、密钥或真实连接串被写入项目文件；扫描排除了 `sn.txt`、构建产物、依赖目录和运行日志。

### 遗留风险

- 当前 SQL 安全层已使用 JSqlParser 加规则校验，但生产级仍需继续增强只读账号隔离、SQL AST 白名单、函数白名单、参数策略、租户权限和字段权限。
- 当前仅支持 PostgreSQL 类型数据源的 SQL 预览。
- 当前操作人仍暂用 `system`，后续权限中心建立后应接入真实 `UserContext`。
- 当前前端只展示预览表格，不做字段自动映射、不做图表渲染、不做发布体检。
- 本机 `8080` 端口仍被旧 Java 进程占用，最新后端验证使用 `18080`。

### 未开发能力

- 未开发字段自动映射。
- 未开发发布前体检、不可变发布版本、浏览态大屏运行时。
- 未开发真实 ECharts、OpenLayers、Babylon.js、G6 渲染。
- 未开发 AI Gateway、完整权限中心和 License Center。
