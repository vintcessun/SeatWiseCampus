# server/12 · 后端测试与验收

- **文档目的**：定义后端测试类型与验收场景，尤其并发红线。
- **适用范围**：`server` 全部功能。
- **读者对象**：后端/测试/Agent。
- **相关文件**：[05-reservation-concurrency-control](05-reservation-concurrency-control.md)、[06-timeout-release-and-blacklist](06-timeout-release-and-blacklist.md)、[../docs/07-acceptance-checklist.md](../docs/07-acceptance-checklist.md)。

## 关键结论
- **并发不双占**是红线，必须有自动化测试；断 Redis 场景也要覆盖。

## 一、测试类型
| 类型 | 覆盖 |
| --- | --- |
| 单元测试 | 时间片换算、状态流转、积分规则、距离/排序 |
| 接口测试 | 各 REST 契约与错误码 |
| 并发测试 | 抢座、单日次数 |
| 超时释放测试 | 延迟任务释放与幂等 |
| 黑名单测试 | 阈值触发与限制 |
| SSE 测试 | 快照 + 事件 + 重连 |
| 报表测试 | 聚合口径 |
| 积分测试(MVP+) | 加减分与流水 |
| 附近空位测试(MVP+) | 过滤与排序 |
| 权限测试 | 角色/属主 |
| 部署测试 | compose 起停 |

## 二、并发验收（红线）
- [ ] **10 个请求同时预约同一座位同一时间段，仅 1 个成功**，其余 `SEAT_ALREADY_RESERVED`。
- [ ] 同一学生超过单日预约次数 → `DAILY_LIMIT_EXCEEDED`。
- [ ] 黑名单用户预约 → `USER_IN_BLACKLIST`。
- [ ] **断开 Redisson/Redis 后并发预约仍不双占**（唯一索引兜底）。

并发测试方法（建议）：
```
CountDownLatch 同步 10 线程同时 POST /api/reservations(同 seat/date/slot)
断言：成功数==1；reservation_slot 对应片只有 1 行；其余得 SEAT_ALREADY_RESERVED
```

## 三、超时释放与签退验收
- [ ] 超时未签到后座位重新变为空闲（FREE），slot 被删除，SSE `seat_released`。
- [ ] **超时未签到后扣积分（-3，MVP+）**且 `no_show_count+1`。
- [ ] 签到成功后超时释放任务不再释放（幂等/已取消）。
- [ ] 主动签退（check-out）：`IN_USE→COMPLETED`，座位释放为 FREE，SSE `seat_released`，（MVP+）+2。
- [ ] **未主动签退时，预约结束时间到达后自动完成**：`IN_USE→COMPLETED` 且座位释放为 FREE（座位不会永久停留在 USING）。
- [ ] 自动完成任务幂等：已签退的预约不重复处理。

## 四、黑名单验收
- [ ] 爽约累计达阈值生成 `blacklist_record` 且 active。
- [ ] 黑名单期内预约被拒；仍可登录/查看历史。
- [ ] 到期/手动解除后恢复预约。

## 五、SSE 验收
- [ ] 首次连接得 `board_snapshot`。
- [ ] 状态变更秒级推 `seat_*`。
- [ ] 断线重连后重取快照，状态与库一致。

## 六、报表验收
- [ ] 报表读 `room_daily_stats`，口径正确，可按维度筛选。

## 七、附近空位验收（MVP+）
- [ ] **最近空位推荐不返回无空位自习室**。
- [ ] 排序符合“同楼栋 > 同校区最近 > 空位更多”。
- [ ] 定位失败 `GEO_LOCATION_REQUIRED`；无结果 `NO_AVAILABLE_ROOM_NEARBY`。

## 八、权限验收
- [ ] 未登录 401；越权 403；操作他人预约 403。

## 九、Docker Compose 测试
- [ ] `docker compose up` 起 mysql/redis/backend；Knife4j 可访问；初始化 SQL 生效。

## 实现约束
- 并发红线用例进 CI；断 Redis 场景可用禁用锁的开关或模拟不可用注入。

## 验收标准
- 本文件红线项（并发不双占、超时释放、黑名单）全部通过，方可进入下一阶段。

## 给 AI Coding Agent 的提示
先写并发与超时释放测试再改相关代码；任何“优化”后重跑红线用例，绿了才提交。
