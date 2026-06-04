# AI_DEVELOPMENT_RULES.md

## 开发红线

1. 不要一次性开发完整系统。
2. 优先完成最小闭环，再逐步增强。
3. 不要破坏已完成的正常功能。
4. 不要写死某一家水务系统的数据结构。
5. 不要把演示数据写死在前端。
6. 所有卡片配置必须可序列化、可保存、可发布、可回滚。
7. 所有接口必须返回统一结构：`success`、`code`、`message`、`data`、`traceId`。
8. 所有异常必须可追踪。
9. 单个卡片异常不能影响整张大屏。
10. 卡片 SQL 只允许 `SELECT`。
11. 禁止执行 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`TRUNCATE`、`CREATE`、`GRANT`、`REVOKE`。
12. 数据库密码、Token、连接串、密钥不得返回前端。
13. 普通用户只看到友好错误提示，管理员可查看脱敏诊断信息。
14. AI 只能分析当前已发布、已拖入、已启用 AI、当前用户有权限的卡片。
15. AI 不得读取未拖入大屏的看板、未发布草稿、未启用 AI 的卡片、无权限字段。
16. 开发完成后必须更新 `CODEX_TASK_LOG.md` 和 `CODEX_NEXT_TASK.md`。

## 补充执行要求

- SQL 必须进行解析、参数化、限行、超时、审计，并生成 `traceId`。
- 固定技术栈不得被擅自替换：PostgreSQL、PostGIS、OpenLayers、Babylon.js、Apache ECharts、@antv/g6、SVG / Iconfont / CSS / Lottie、AI Gateway。
- AI 上下文必须先经过发布状态、卡片启用状态、AI 启用状态、用户权限和字段权限过滤，再进入 Dashboard Context Manifest。
- License Center、组件签名、资产水印、反篡改、授权审计等知识产权保护机制不得被绕过。
- 修改代码前必须完成 `AGENTS.md` 规定的开发前读取与输出。
