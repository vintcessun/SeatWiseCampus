# server/13 · 后端实现指南（面向 Coding Agent）

- **文档目的**：给后端 Coding Agent 明确的实现顺序、包结构、事务/锁/唯一索引落点与 mock/测试策略。
- **适用范围**：`server` 编码阶段。
- **读者对象**：Codex/Claude Code/opencode。
- **相关文件**：[../ROADMAP.md](../ROADMAP.md)、[05-reservation-concurrency-control](05-reservation-concurrency-control.md)、[02-database-schema](02-database-schema.md)、[AGENTS.md](AGENTS.md)。

## 关键结论
- 按 ROADMAP 阶段推进；预约路径必须锁 + 事务 + 唯一索引；先写并发/超时测试。

## 一、推荐实现顺序
1. 脚手架：Spring Boot 3.5 + MyBatis-Plus + Sa-Token + Redisson + Knife4j + 统一响应/异常/错误码。
2. 建表与实体（P1/P2）：sys_user→campus→building→study_room→seat→reservation→reservation_slot→blacklist_record→(score_record/room_daily_stats)。
3. 鉴权（P1）：登录/角色/属主。
4. 基础数据 CRUD（P1）+ 座位排布（P2）。
5. **预约并发（P3）**：时间片换算 → Redisson 锁 → 事务插入 → 唯一索引兜底。
6. 签到/取消 + 超时释放 + 黑名单（P4）。
7. SSE 看板（P5）。
8. 报表聚合（P6）。
9. 【MVP+】积分（P7）、附近空位（P8）。

## 二、包结构
见 [00](00-server-overview.md)：controller/service/mapper/entity/dto/vo/config/job/sse/security/common。

## 三、Entity 生成顺序
User → Campus → Building → StudyRoom → Seat → Reservation → ReservationSlot → BlacklistRecord → ScoreRecord → RoomDailyStats → OperationLog。

## 四、Mapper 生成顺序
与 Entity 对应，MyBatis-Plus `BaseMapper`；ReservationSlotMapper 支持批量插入与按 reservation_id 删除。

## 五、Service 生成顺序
AuthService → Campus/Building/RoomService → SeatLayoutService → **ReservationService（核心）** → CheckInService（签到）→ CheckOutService（签退/自动完成）→ TimeoutReleaseService → BlacklistService → BoardSseService → ReportService →（ScoreService/NearestRoomService）。

> 座位生命周期务必闭合：`PENDING_SIGN_IN`→（签到）`IN_USE`→（签退或结束自动完成）`COMPLETED` 并释放座位。CheckInService 成功时需投递「自动完成任务」（到期=预约结束时间）；否则 USING 座位无法回收（见 [06 §八](06-timeout-release-and-blacklist.md)）。

## 六、Controller 生成顺序
与 Service 对应；保持瘦，只做参数校验与转发。

## 七、必须 @Transactional 的地方
| 操作 | 事务 |
| --- | --- |
| 预约(reservation + slots) | 是，同事务 |
| 超时释放(状态+删slot+计数+扣分) | 是 |
| 签退/自动完成(状态→COMPLETED+删slot+释放座位+积分) | 是 |
| 取消(状态+删slot+可选扣分) | 是 |
| 签到(状态+时间) | 是 |
| 排布保存(seat upsert) | 是 |
| 积分结算 | 是(与来源事务一致或紧随) |

## 八、必须 Redisson 锁的地方
- 预约提交：`seat:{seatId}:date:{date}:slots:{range}`。
- （可选）排布保存对同房间加锁避免并发覆盖。
锁在 service 层显式获取，try-finally 释放。

## 九、必须依赖唯一索引的地方
- `reservation_slot(seat_id,date,slot_index)`：插入判重，DuplicateKey → `SEAT_ALREADY_RESERVED`。**这是不可移除的兜底。**

## 十、哪些可以先 mock
- 报表数据（聚合任务未就绪前返回样例）、积分排行、附近空位可先返回占位结构（结构须与 [03](03-api-design.md) 一致）。
- AI/通知仅接口占位。

## 十一、哪些必须先写测试
- 预约并发（10 并发仅 1 成功）、超时释放幂等、黑名单阈值——先写测试再实现（见 [12](12-server-test-and-acceptance.md)）。

## 十二、不要一次性实现所有扩展
- 严格按阶段；MVP 阶段可建 score/location 表与预留钩子，但不实现 P7/P8 业务逻辑。

## 十三、每次改动后更新文档
- 字段→[02](02-database-schema.md)；接口→[03](03-api-design.md)；规则/状态机→[01](01-domain-model.md)；错误码→[03](03-api-design.md)+[../GLOSSARY.md](../GLOSSARY.md)。

## 实现约束
- 遵守 [AGENTS.md](AGENTS.md) 全部硬性规则；红线测试绿灯才提交。

## 验收标准
- 每阶段产物对照 [12](12-server-test-and-acceptance.md) 与 [../docs/07](../docs/07-acceptance-checklist.md) 通过。

## 给 AI Coding Agent 的提示
从预约核心（步骤见 [05](05-reservation-concurrency-control.md)）入手最能锁定架构；先测试后实现，保持每步可运行。
