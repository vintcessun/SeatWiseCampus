# server/AGENTS.md · 后端 Agent 规则（仅约束后端）

> 只约束 `server/`。通用规则见 [../AGENTS.md](../AGENTS.md)，架构见 [../docs/02-system-architecture.md](../docs/02-system-architecture.md)。

## 修改 server 前必须阅读
| 改动 | 必读 |
| --- | --- |
| 任意 | 本文件 + [00-server-overview](00-server-overview.md) + [01-domain-model](01-domain-model.md) + [../GLOSSARY.md](../GLOSSARY.md) |
| 预约相关 | [05-reservation-concurrency-control](05-reservation-concurrency-control.md) |
| 超时释放相关 | [06-timeout-release-and-blacklist](06-timeout-release-and-blacklist.md) |
| SSE 相关 | [07-sse-realtime-board](07-sse-realtime-board.md) |
| 鉴权相关 | [04-auth-rbac](04-auth-rbac.md) |
| 报表相关 | [08-statistics-reporting](08-statistics-reporting.md) |

## 硬性规则
1. **不允许删除 `reservation_slot` 的 `(seat_id,date,slot_index)` 唯一索引兜底。**
2. **不允许只靠 Redis 判断最终预约成功**；Redis 不可用时正确性仍由 MySQL 保证。
3. **不允许只靠前端校验**；所有规则后端强制。
4. 预约写路径必须：Redisson 锁 + `@Transactional` + 唯一索引兜底。
5. 超时释放以 Redisson DelayedQueue / Redis ZSet 为主，全表扫描仅兜底。
6. 状态流转严格按状态机（[01](01-domain-model.md)），不新增图外转移。
7. 分级实现：MVP → MVP+（09/10）→ 扩展；不越级。

## 实现后必须更新文档
- 字段变更 → [02-database-schema](02-database-schema.md)。
- 接口变更 → [03-api-design](03-api-design.md)。
- 业务规则/状态机变更 → [01-domain-model](01-domain-model.md)。
- 新错误码 → [03](03-api-design.md) + [../GLOSSARY.md](../GLOSSARY.md)。

## 代码风格
- 分层：controller→service→mapper；实体 entity，出入参 dto/vo。
- 写库方法 `@Transactional`；锁在 service 层显式获取/释放（try-finally）。
- 统一响应结构与业务错误码；异常经全局处理器转错误码。
- 命名遵循 GLOSSARY；表 `snake_case`，类 `PascalCase`。

## 提交前 checklist
- [ ] 预约路径保留锁 + 唯一索引兜底
- [ ] 断 Redis 不双占（有测试）
- [ ] 状态流转合法
- [ ] 字段/接口/规则变更已回写文档
- [ ] 未越级实现扩展

## 不应该做的事
- 不要用 Redis 计数替代唯一索引做去重。
- 不要在无锁/无事务下批量写 slot。
- 不要每分钟扫全表作为超时释放主方案。
- 不要一次性实现所有扩展或重写工程。
