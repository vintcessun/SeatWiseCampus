# AGENTS.md · 通用 Coding Agent 规则

> 适用于 Codex、opencode 及其他通用 Coding Agent。Claude Code 另见 [CLAUDE.md](CLAUDE.md)。
> 本文件只写**核心规则 + 文档路由 + 修改约束**，详细设计在 `docs/`、`client/`、`server/` 中。

## 一句话描述
SeatWise Campus 是一套 C/S 架构（`client` 前端 + `server` 后端）的高校自习室在线预约系统，核心是**时间片并发选座**与**实时座位看板**。

## 必须遵守的规则
1. **前端绝不直接访问数据库**，只能通过 REST API / SSE 与后端通信。
2. **座位是否预约成功的最终结论只能由后端给出**，前端校验仅用于体验优化。
3. **并发控制不可绕过**：预约必须走 Redisson 锁 + MySQL 唯一索引兜底；禁止“先查空位再插入”作为唯一手段。
4. **Redis 不是正确性来源**，MySQL `reservation_slot` 的唯一索引才是最终兜底，不得删除。
5. **分级实现**：先 MVP，再 MVP+（积分/附近空位），最后扩展（AI/通知）；禁止一次性实现全部扩展。
6. **小步修改，保持可运行**：不要一次性重写整个工程。
7. **术语统一**：StudyRoom / Seat / Reservation / ReservationSlot / BlacklistRecord / Score / NearestAvailableRoom，详见 [GLOSSARY.md](GLOSSARY.md)。
8. 文档用中文；类名、表名、接口路径、枚举值用英文。

## client / server 职责边界
| 端 | 负责 | 禁止 |
| --- | --- | --- |
| client | 展示、交互、表单校验、API/SSE 调用 | 写死业务规则、判定预约最终成功、直连数据库 |
| server | 业务规则、权限、并发、数据一致性、SSE 推送 | 把正确性交给前端或仅靠 Redis |

## 修改前必须先读
| 你要改的东西 | 先读 |
| --- | --- |
| 任何改动 | 本文件 + [GLOSSARY.md](GLOSSARY.md) + [docs/02-system-architecture.md](docs/02-system-architecture.md) |
| 前端 | [client/AGENTS.md](client/AGENTS.md) |
| 后端 | [server/AGENTS.md](server/AGENTS.md) |
| 预约/并发 | [server/05-reservation-concurrency-control.md](server/05-reservation-concurrency-control.md) |
| 超时/黑名单 | [server/06-timeout-release-and-blacklist.md](server/06-timeout-release-and-blacklist.md) |
| SSE/看板 | [server/07-sse-realtime-board.md](server/07-sse-realtime-board.md) |
| 数据库/接口 | [server/02-database-schema.md](server/02-database-schema.md) / [server/03-api-design.md](server/03-api-design.md) |

## 实现后必须更新的文档
- 改字段 → 更新 `server/02-database-schema.md`
- 改接口 → 更新 `server/03-api-design.md`
- 改业务规则/状态机 → 更新 `server/01-domain-model.md`
- 新增页面 → 更新 `client/01-page-route-map.md`
- 新增组件 → 更新 `client/05-component-design.md`
- 新增接口调用 → 更新 `client/07-api-calling-design.md`
- 新增 Pinia store → 更新 `client/06-state-management.md`

## 命令占位（脚手架落地后填充）
```bash
# server
# cd server && ./mvnw spring-boot:run
# cd server && ./mvnw test
# 后端接口测试：Knife4j http://localhost:8080/doc.html

# client
# cd client && npm install && npm run dev
# cd client && npm run test
# cd client && npm run build
```

## 代码风格
- 后端：分层 controller/service/mapper/entity/dto/vo；写库操作 `@Transactional`；对外统一响应结构与业务错误码。
- 前端：组合式 API（`<script setup>`）；API 集中在 `api/` 模块；状态集中在 Pinia；不在组件里硬编码 baseURL。
- 命名：数据库 `snake_case`，Java 类 `PascalCase`，接口路径 `/api/...` kebab/复数资源。

## 提交前 checklist
- [ ] 未让前端承担正确性判定
- [ ] 预约路径保留 Redisson 锁 + 唯一索引兜底
- [ ] 改动范围小、工程可运行
- [ ] 相关文档已同步更新
- [ ] 未越界实现未排期的扩展功能

## Agent 不应该做的事
- 不要删除 `reservation_slot` 唯一索引或用 Redis 替代它。
- 不要一次性生成超大文件或重写整个项目。
- 不要在需求不明确时臆造规则——先查文档，仍不清晰则在产出中标注 `TODO(待确认)`。
- 不要在本阶段实现业务代码（当前仅文档阶段），除非用户明确要求进入某个 P 阶段。
