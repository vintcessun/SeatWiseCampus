# server · 后端文档总览

- **文档目的**：后端工程文档入口，给出定位、技术栈、模块与阅读顺序。
- **适用范围**：`server/` 后端工程。
- **读者对象**：后端/Agent。
- **相关文件**：[server/AGENTS.md](AGENTS.md)、[00-server-overview](00-server-overview.md)、[../docs/02-system-architecture.md](../docs/02-system-architecture.md)。

## 关键结论
- 后端是系统唯一权威：业务规则、并发、权限、一致性、SSE 推送都在此。
- 正确性来源是 **MySQL**（尤其 `reservation_slot` 唯一索引），Redis 仅加速与调度。

## 一、后端定位
承载 SeatWise Campus 全部业务逻辑，对 client 暴露 REST + SSE。

## 二、技术栈
JDK 21 + Spring Boot 3.5.x + MyBatis-Plus + MySQL 8 + Redis 7 + Redisson + Sa-Token + Knife4j + Docker Compose。

## 三、模块划分
controller / service / mapper / entity / dto / vo / config / job / sse / security / common。详见 [00](00-server-overview.md)。

## 四、文档索引
| 文件 | 说明 |
| --- | --- |
| [00-server-overview](00-server-overview.md) | 服务端整体架构 |
| [01-domain-model](01-domain-model.md) | 领域模型与状态机 |
| [02-database-schema](02-database-schema.md) | MySQL 表设计 |
| [03-api-design](03-api-design.md) | REST API |
| [04-auth-rbac](04-auth-rbac.md) | 认证鉴权 |
| [05-reservation-concurrency-control](05-reservation-concurrency-control.md) | 预约并发控制（核心） |
| [06-timeout-release-and-blacklist](06-timeout-release-and-blacklist.md) | 超时释放与黑名单 |
| [07-sse-realtime-board](07-sse-realtime-board.md) | SSE 实时看板 |
| [08-statistics-reporting](08-statistics-reporting.md) | 统计报表 |
| [09-score-ranking-design](09-score-ranking-design.md) | 积分排名(MVP+) |
| [10-nearest-available-room-design](10-nearest-available-room-design.md) | 最近空位推荐(MVP+) |
| [11-deployment-docker-compose](11-deployment-docker-compose.md) | 部署 |
| [12-server-test-and-acceptance](12-server-test-and-acceptance.md) | 测试验收 |
| [13-llm-server-implementation-guide](13-llm-server-implementation-guide.md) | 实现指南(LLM) |

## 五、推荐阅读顺序
00 架构 → 01 领域模型 → 02 数据库 → 03 API → 04 鉴权 → 05 并发 → 06 超时/黑名单 → 07 SSE → 08 报表 →（09/10 扩展）→ 11 部署 → 13 实现指南。

## 六、MVP 范围
登录/基础数据/座位/预约并发/签到/超时释放/黑名单/SSE 看板/报表。

## 七、扩展范围
积分排名(09)、最近空位(10)、AI/通知（[../docs/05](../docs/05-extension-design.md)）。

## 给 AI Coding Agent 的提示
改后端先读 [AGENTS.md](AGENTS.md)；动预约/超时/SSE 必读对应 05/06/07。
