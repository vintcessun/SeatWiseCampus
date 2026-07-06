# CLAUDE.md · Claude Code 规则

> 面向 Claude Code。只写核心规则与文档索引，详细设计跳转 `docs/` `client/` `server/`。通用规则见 [AGENTS.md](AGENTS.md)。

## 进入项目后的阅读顺序
1. [README.md](README.md) — 项目是什么。
2. [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) — 全局上下文、角色、MVP 边界。
3. [GLOSSARY.md](GLOSSARY.md) — 术语，避免命名漂移。
4. [docs/02-system-architecture.md](docs/02-system-architecture.md) — 架构与边界。
5. 按任务方向：前端读 [client/AGENTS.md](client/AGENTS.md)，后端读 [server/AGENTS.md](server/AGENTS.md)。
6. [ROADMAP.md](ROADMAP.md) — 确认当前处于哪个 P 阶段。

## 架构摘要
- C/S：`client`（Vue3+Vite+Element Plus+ECharts+Pinia）↔ REST/SSE ↔ `server`（Spring Boot 3.5 + MyBatis-Plus + MySQL8 + Redis7 + Redisson + Sa-Token）。
- 预约用 **30 分钟时间片**；`reservation` 主记录 + `reservation_slot` 时间片占用；`seat_id + date + slot_index` 唯一。
- 并发：Redisson 锁减少冲突 + MySQL 唯一索引最终兜底；Redis 仅缓存/锁/延迟队列。
- 实时看板：初始化快照 + SSE 增量推送。
- 超时：预约开始后 15 分钟未签到自动释放，用 Redisson DelayedQueue / Redis ZSet。

## 修改 client 时
- 只做交互与表单校验，**结果以后端返回为准**；不要在前端判定预约最终成功。
- 座位状态来自「快照 + SSE」，不要前端自行推算占用。
- 新增页面/组件/接口/store，同步更新对应 `client/*` 文档。

## 修改 server 时
- 动预约逻辑先读 [server/05-reservation-concurrency-control.md](server/05-reservation-concurrency-control.md)，保留锁 + 唯一索引。
- 动超时/黑名单先读 [server/06-timeout-release-and-blacklist.md](server/06-timeout-release-and-blacklist.md)。
- 动 SSE 先读 [server/07-sse-realtime-board.md](server/07-sse-realtime-board.md)。
- 改字段/接口/规则，同步更新 `server/02` `server/03` `server/01`。

## 新增功能时必须更新文档
先改文档（数据库/接口/领域模型/页面/组件），再写代码，保持文档与实现一致。

## 需求不明确时
先在文档中查证；仍不清晰时，**不要臆造**——用 `AskUserQuestion` 提出关键选择，或在产出中以 `TODO(待确认: ...)` 标注并给出保守默认。

## 工作方式
- **不要一次性重写整个项目**；优先小步修改、保持每步可运行、可验证。
- 优先 MVP → MVP+ → 扩展，逐 P 推进，不越界实现未排期功能。
- **当前为文档阶段，未经用户明确指示不实现业务代码。**

## 文档索引
- 通用：[docs/](docs/)（`00`~`07`）
- 前端：[client/](client/)（`00`~`10`）
- 后端：[server/](server/)（`00`~`13`）
