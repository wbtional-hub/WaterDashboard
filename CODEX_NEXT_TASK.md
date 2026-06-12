# CODEX_NEXT_TASK.md

## 下一次任务

第一阶段第 4 步：开发组件模板管理基础能力。

下一次开发前必须执行 `AGENTS.md` 中规定的强制读取和强制输出流程。

## 下一步任务范围

1. 基于 `platform.component_template` 和 `platform.component_template_version` 建立组件模板后端基础接口。
2. 支持组件模板列表、详情、新增、编辑、启用、停用。
3. 支持创建模板版本，保存 `data_contract_json`、`default_config_json`、`field_mapping_schema_json`、`checksum` 等字段。
4. 第一阶段优先支持普通图表模板，渲染引擎使用 `ECHARTS`，只做模板元数据和契约管理。
5. 前端新增组件模板管理基础页面，支持列表、筛选、新增、编辑、版本信息查看。
6. 预留 `license_scope`、`signature`、`checksum`，但不开发完整 License Center。
7. 所有接口继续返回 `success`、`code`、`message`、`data`、`traceId`。
8. 操作写入 `platform.audit_log`，异常继续脱敏并可追踪。

## 下一步禁止事项

1. 不要一次性开发全部页面。
2. 不要开发完整 AI 模型调用。
3. 不要开发复杂 GIS 和三维。
4. 不要写死演示数据。
5. 不要绕过 `AI_MEMORY.md` 和 `AI_DEVELOPMENT_RULES.md`。
6. 不要破坏 `AGENTS.md` 约束。
7. 不要将真实数据库密码、Token、连接串或密钥提交到仓库。
8. 不要在平台配置库中写死某一家水务系统的数据结构。
9. 不要通过关闭 PostGIS 校验或手工绕过 Flyway 来伪造迁移成功。
10. 不要开发卡片 SQL 预览执行功能。
11. 不要开发大屏编辑器、AI Gateway、GIS、三维、G6、完整权限中心或 License Center。
12. 不要把模板配置写死成不可替换的水务业务页面。
