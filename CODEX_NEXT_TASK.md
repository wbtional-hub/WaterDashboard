# CODEX_NEXT_TASK.md

## 下一次任务

第一阶段第 8 步：开发卡片数据源选择与 SQL 配置骨架。

下一次开发前必须执行 `AGENTS.md` 中规定的强制读取和强制输出流程。

## 下一步任务范围

1. 在编辑器卡片属性面板中增加数据绑定配置入口。
2. 支持为卡片选择已启用的数据源。
3. 支持填写 `SELECT` SQL 文本，但本步骤默认只保存配置，不执行 SQL。
4. 支持保存卡片 `dataBinding` 配置到 `dashboard_card.config_json` 和 `dashboard_draft.config_json.cards`。
5. 预留字段映射配置结构，但不做完整字段映射器。
6. 增加 SQL 配置的基础前端提示：只能使用 `SELECT`，禁止写入类 SQL。
7. 后端必须对 SQL 文本做第一层红线校验，禁止 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`TRUNCATE`、`CREATE`、`GRANT`、`REVOKE`。
8. 明确记录：关键字校验不是最终安全方案，后续 SQL 预览执行前必须接入 SQL 解析器、只读账号、超时、限行和审计。
9. 所有保存操作继续写入 `platform.audit_log`。
10. 所有接口继续返回 `success`、`code`、`message`、`data`、`traceId`。
11. 保持数据源配置中心、组件模板管理、大屏管理和编辑器卡片草稿能力不受影响。

## 下一步禁止事项

1. 不要执行卡片 SQL。
2. 不要开发 SQL 预览。
3. 不要开发字段映射完整闭环。
4. 不要开发发布版本、发布体检或浏览态大屏。
5. 不要开发真实拖拽、缩放、吸附线或图层管理。
6. 不要开发真实 OpenLayers 地图卡、Babylon.js 三维卡或 G6 拓扑卡。
7. 不要开发 AI Gateway、完整权限中心或 License Center。
8. 不要把演示数据写死在前端。
9. 不要写死某一家水务系统的数据结构。
10. 不要将真实数据库密码、Token、连接串或密钥提交到仓库。
11. 不要绕过 `AI_MEMORY.md` 和 `AI_DEVELOPMENT_RULES.md`。
12. 不要破坏 `AGENTS.md` 约束。
13. 不要修改已执行的 `V1` 至 `V7` 迁移脚本；如需调整，只能新增迁移。
