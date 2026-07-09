# 16 · 组队相邻预约（原子多座）设计

> 增强功能 ⑥。一次为多名同学预约**同一自习室、同一时段的多个相邻座位**，
> 要么全部成功、要么整体回滚——用来展示分布式并发下的**原子性**，是核心并发设计
> （Redisson 锁 + MySQL 唯一索引）的最强演示点。关联 [05-reservation-concurrency-control.md](05-reservation-concurrency-control.md)。

## 领域模型
- 不新增表。每个座位仍生成一条独立 `reservation`（状态机、签到、超时释放完全复用）。
- 「组队」只是一次**原子批量创建**：N 个座位分别归属 N 名成员（含发起人）。
- 之所以按成员拆分而非发起人独占：单人同一时段多座会触发「自身时段冲突」规则，
  且组队语义本就是「多人相邻自习」。

## 接口
`POST /api/reservations/group`（角色 STUDENT）
```json
{
  "roomId": 1, "date": "2026-07-10", "startTime": "14:00", "endTime": "16:00",
  "members": [
    { "seatId": 11, "username": "student1" },
    { "seatId": 12, "username": "student2" },
    { "seatId": 13, "username": "student3" }
  ]
}
```
返回：成功为 `List<ReservationVO>`（每座一条）；任一冲突则整体失败，
`code=SEAT_ALREADY_RESERVED`，`message` 指明被抢的座位号。

## 校验（批量创建前）
1. `members` 非空、`size ≤ groupMaxSeats`（配置，默认 6）；`seatId`、`username` 均不重复。
2. 所有 `seatId` 属于 `roomId`、为可预约 `SEAT`；**相邻**：同一 `rowIndex`，`colIndex` 连续（允许跨过道？MVP 要求严格连续同排）。
3. 每个 `username` 存在且为 STUDENT；逐一复用单座校验：黑名单、开放时间、开始晚于当前、单次时长、单日次数、自身时段冲突。
4. 时间对齐、`startSlot < endSlot`、不超过 `maxSlotsPerReservation`。

## 并发与原子性（关键）
1. 对所有 `seatId` **按升序**取 Redisson 锁（`MultiLock` 或有序逐个 `tryLock`），避免不同组交叉加锁死锁。
2. 在**单个事务**内，为每个成员插入 `reservation` + 其 `reservation_slot`；
   任一 `reservation_slot` 命中唯一键 `uk_seat_date_slot` → `DuplicateKeyException` → **整个事务回滚**（全部不生效）。
3. 事务成功后：逐座清理临时锁座 `hold:`、SSE 广播 `seat_reserved`、给每位成员发通知（type=`GROUP`）。
4. 释放全部锁（`finally`）。

## 前端
- 选座页新增「组队预约」开关：开启后可多选相邻空位；每个选中座位分配一名成员（用户名输入/下拉）。
- 提交调用 `reservationApi.group(...)`；成功提示「N 座已为小组锁定」，失败提示被抢座位并刷新看板。

## 配置
`seatwise.group-max-seats`（默认 6）。

## 验收
- 并发脚本：两组同时抢包含重叠座位的相邻区间，**恰好一组整体成功、另一组整体失败**，
  且失败组**不留任何** `reservation`（原子性）。见 `scripts/test-group.mjs`。
