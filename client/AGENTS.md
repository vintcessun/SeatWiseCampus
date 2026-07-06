# client/AGENTS.md · 前端 Agent 规则（仅约束前端）

> 只约束 `client/`。通用规则见 [../AGENTS.md](../AGENTS.md)，架构见 [../docs/02-system-architecture.md](../docs/02-system-architecture.md)。

## 修改 client 前必须阅读
| 改动 | 先读 |
| --- | --- |
| 任意 | 本文件 + [00-client-overview](00-client-overview.md) + [../GLOSSARY.md](../GLOSSARY.md) |
| 新页面/路由 | [01-page-route-map](01-page-route-map.md) |
| 选座/看板 | [04-seat-grid-and-heatmap](04-seat-grid-and-heatmap.md) |
| 组件 | [05-component-design](05-component-design.md) |
| 状态 | [06-state-management](06-state-management.md) |
| 接口调用/SSE | [07-api-calling-design](07-api-calling-design.md) |
| 校验/报错 | [08-frontend-validation-and-error-handling](08-frontend-validation-and-error-handling.md) |

## 硬性规则
1. **不允许在前端写死业务规则绕过后端**（单日次数、黑名单、签到窗口、并发判定等只作提示，最终以后端为准）。
2. **不允许前端判定预约最终成功**：提交后以后端响应为准，成功前座位不得视为已锁定。
3. 前端只做**交互校验**（时间不能选过去、已占座位禁选等），用于体验，不替代后端校验。
4. 座位状态来源唯一：**初始化快照 + SSE 增量**；不要用本地推算覆盖服务端事件。
5. baseURL、token 注入、错误码处理集中在 API 层，组件内不硬编码。

## 实现后必须更新文档
- 新增页面 → 更新 [01-page-route-map](01-page-route-map.md)。
- 新增组件 → 更新 [05-component-design](05-component-design.md)。
- 新增接口调用 → 更新 [07-api-calling-design](07-api-calling-design.md)。
- 新增/修改 Pinia store → 更新 [06-state-management](06-state-management.md)。

## 代码风格
- Vue 3 `<script setup>` 组合式 API；组件 `PascalCase`。
- API 调用集中在 `src/api/*`；状态在 `src/stores/*`；路由在 `src/router`。
- Element Plus 表单用其校验规则；错误提示统一走全局拦截。

## 提交前 checklist
- [ ] 未在前端做最终正确性判定
- [ ] 座位状态仅由快照+SSE 驱动
- [ ] 新增页面/组件/接口/store 已回写文档
- [ ] 401/403/业务错误码有统一处理
- [ ] 未越界实现未排期扩展页面

## 不应该做的事
- 不要直连数据库/缓存（前端根本不应有此能力）。
- 不要为“看起来更快”而在前端缓存并信任座位占用状态。
- 不要一次性生成巨型页面文件；按组件拆分。
