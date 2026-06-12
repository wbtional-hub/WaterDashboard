# CODEX_NEXT_TASK.md

## 下一次任务

第一阶段第 10 步：开发字段映射配置基础能力。

下一次开发前必须执行 `AGENTS.md` 中规定的强制读取和强制输出流程。

## 前置状态

- 第一阶段第 9 步卡片 SQL 预览执行器基础能力已完成。
- 卡片已可保存数据源绑定、`SELECT` SQL、字段映射占位和刷新配置。
- SQL 预览已具备 JSqlParser 解析校验、危险语句拦截、多语句拦截、只读连接、5 秒超时、最大行数限制、`sqlHash`、`traceId` 和 `card_query_log` 记录。
- SQL 预览不会递增 `dashboard_draft.revision`，不会写入 `dashboard_version`，不会把预览结果持久化进卡片配置。
- 下一次开发不得修改已执行成功的 `V1` 到 `V8` 历史迁移脚本；如需调整数据库结构，只能新增迁移。

## 下一步任务范围

1. 在编辑器右侧属性面板完善字段映射配置区域。
2. 基于 SQL 预览返回的 columns，支持用户手工维护 `fieldMapping`。
3. 支持按模板数据契约提示需要映射的字段，但本阶段不做 AI 自动映射。
4. 支持保存 `fieldMapping` 到 `dashboard_card.config_json.dataBinding.fieldMapping`。
5. 同步更新 `dashboard_draft.config_json.cards` 中对应卡片摘要。
6. 保存字段映射时递增 `dashboard_draft.revision`。
7. 字段映射保存必须写入 `audit_log`。
8. 字段映射配置不得保存密码、Token、连接串、密钥或未授权字段。
9. 保持数据源配置中心、组件模板管理、大屏管理、编辑器、卡片实例、SQL 配置和 SQL 预览能力不受影响。

## 下一步禁止事项

1. 不要开发真实图表渲染。
2. 不要开发发布前体检。
3. 不要开发发布版本。
4. 不要开发浏览态大屏。
5. 不要开发真实 OpenLayers 地图卡。
6. 不要开发真实 Babylon.js 三维卡。
7. 不要开发真实 G6 拓扑卡。
8. 不要开发 AI Gateway。
9. 不要开发完整权限中心。
10. 不要开发 License Center。
11. 不要绕过 `AI_MEMORY.md` 和 `AI_DEVELOPMENT_RULES.md`。
12. 不要破坏 `AGENTS.md` 约束。
13. 不要提交 `.env`、真实密码、Token、密钥或真实连接串。

## 下一步验收建议

- `mvn test` 通过。
- `mvn package -DskipTests` 通过。
- `npm run build` 通过。
- `/dashboards/:id/editor` 可在 SQL 预览后展示字段列表。
- 字段映射可保存并回显。
- `dashboard_card.config_json.dataBinding.fieldMapping` 已更新。
- `dashboard_draft.config_json.cards` 已同步更新。
- `dashboard_draft.revision` 正常递增。
- `dashboard_version` 未写入。
- `audit_log` 有字段映射保存记录。
- 接口响应包含 `traceId`，且不暴露敏感信息。
