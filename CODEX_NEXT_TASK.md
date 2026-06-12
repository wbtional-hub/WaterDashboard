# CODEX_NEXT_TASK.md

## 下一次任务

第一阶段第 9 步：开发卡片 SQL 预览执行器基础能力。

下一次开发前必须执行 `AGENTS.md` 中规定的强制读取和强制输出流程。

## 前置状态

- 第一阶段第 8 步卡片数据源选择与 SQL 配置骨架已完成。
- 卡片 `dataBinding`、`fieldMapping` 和 `refresh` 已可保存到 `dashboard_card.config_json` 并同步到 `dashboard_draft.config_json.cards`。
- 当前只完成配置级 SQL 红线校验，尚未执行 SQL，尚未做 SQL 解析器级安全校验。
- 下一次开发不得修改已执行的 `V1` 至 `V7` 迁移脚本；如需调整数据库结构，只能新增迁移。

## 下一步任务范围

1. 建立后端 `sql-security` 或等价 SQL 安全服务。
2. 引入或封装 SQL 解析器能力，禁止仅依赖关键字字符串拦截。
3. 只允许单条 `SELECT` 或安全 `WITH ... SELECT` 查询。
4. 禁止写入型 SQL、多语句、存储过程、DDL、权限语句和危险函数。
5. SQL 预览必须使用数据源配置中的受控连接，不返回密码、Token、连接串或密钥。
6. SQL 预览必须设置查询超时和最大返回行数。
7. SQL 预览必须记录 `card_query_log`，包含 `dashboardId`、`cardId`、`dataSourceId`、`sqlHash`、耗时、结果、错误摘要和 `traceId`。
8. 前端可新增或启用“SQL 预览”按钮，但只显示预览字段和少量样例数据，不做真实卡片渲染。
9. 普通错误提示不得展示 SQL 原文、连接串或完整堆栈。
10. 保持数据源配置中心、组件模板管理、大屏管理、编辑器、卡片实例和卡片数据绑定配置能力不受影响。

## 下一步禁止事项

1. 不要支持 INSERT、UPDATE、DELETE、DROP、ALTER、TRUNCATE、CREATE、GRANT、REVOKE、EXECUTE、CALL。
2. 不要支持多语句 SQL。
3. 不要使用平台库账号执行外部业务 SQL。
4. 不要返回数据库密码、Token、连接串、密钥或完整异常堆栈。
5. 不要开发发布体检、发布版本或浏览态大屏。
6. 不要开发真实 ECharts/OpenLayers/Babylon.js/G6 渲染。
7. 不要开发 AI Gateway、完整权限中心或 License Center。
8. 不要把演示数据写死在前端。
9. 不要写死某一家水务系统的数据结构。
10. 不要绕过 `AI_MEMORY.md` 和 `AI_DEVELOPMENT_RULES.md`。
11. 不要破坏 `AGENTS.md` 约束。
