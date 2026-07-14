# pre/02 · API 脚本要模拟的数据与操作（对应 Q2）

- **文档目的**：明确演示下层「API 脚本」需要模拟哪些数据与多用户操作，给出复用矩阵与新演示驱动脚本的规格/伪代码。
- **适用范围**：并发/背景/时间压缩层的脚本设计。
- **读者对象**：编写演示脚本的 Agent、演示者。
- **相关文件**：[00-overview.md](00-overview.md)、[01-feature-catalog.md](01-feature-catalog.md)、[04-orchestration.md](04-orchestration.md)、[../scripts/seed-demo.mjs](../scripts/seed-demo.mjs)、[../scripts/smoke-test.mjs](../scripts/smoke-test.mjs)、[../scripts/test-hold.mjs](../scripts/test-hold.mjs)、[../scripts/test-waitlist.mjs](../scripts/test-waitlist.mjs)、[../scripts/test-group.mjs](../scripts/test-group.mjs)。

---

## 关键结论

- **复用范式、不复用测试脚本本身**：现有 `scripts/*.mjs` 的 `api()/login()/tomorrow()` + `satoken` 头 + `Promise.all` 并发是最佳蓝本；但 `test-*.mjs` 是断言测试（输出简陋、`process.exit`、消耗每日限次、需干净库），不宜直接作为演示驱动。
- **可直接复用**：`seed-demo.mjs`、`seed-replay.mjs`（纯造数）、`smoke-test.mjs` 的并发段（现场跑很直观）。
- **新写一组幂等、可参数化、按录制节奏的演示驱动脚本**（本文件只给规格与伪代码，不落地）。

---

## 一、通用范式（照抄现有脚本，务必一致）

所有脚本共用如下最小封装（源自 [../scripts/seed-demo.mjs](../scripts/seed-demo.mjs) 等）：

```js
const BASE = process.env.BASE || 'http://localhost:8888'   // 经 nginx 同源代理到后端
async function api(path, opt = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (opt.token) headers['satoken'] = opt.token             // REST 用请求头
  const res = await fetch(BASE + path, {
    method: opt.method || 'GET', headers,
    body: opt.body ? JSON.stringify(opt.body) : undefined,
  })
  return res.json().catch(() => ({}))
}
const login = async (u, p) => (await api('/api/auth/login', { method:'POST', body:{ username:u, password:p } })).data?.token
const tomorrow = () => { const d = new Date(); d.setDate(d.getDate()+1); return d.toISOString().slice(0,10) }
// 约定：响应 { code, message, data }，code === '0' 为成功
// SSE 例外：/api/board/stream?...&token=<token>（EventSource 不能带头，token 走查询参数）
```

**关键约束（写脚本前必读）**：
- 每日限次 `SEATWISE_DAILY_LIMIT=3`，单预约 ≤ 8 个时间片（4 小时）。演示反复抢座会耗尽某学生当日额度——**多学生轮换或每次换时段**。
- 30 分钟一个时间片；`seatId + date + slot_index` 唯一。
- 超时释放需签到窗口到期；正常后端为 15 分钟，压缩演示须用 `:18081` 短窗口后端。
- 候补/组队/并发的「恰好一个成功」类断言**需要干净库**，先 `docker compose down -v && up -d`。

## 二、复用矩阵：现有脚本 ↔ 覆盖功能 ↔ 可用性

| 现有脚本 | 覆盖功能 | 演示中如何用 | 注意事项 |
| --- | --- | --- | --- |
| [`seed-demo.mjs`](../scripts/seed-demo.mjs) | 明天 A301 多状态预约（待签到/使用中/已完成/已取消/占位） | **直接复用**造看板初始态 | 写入数据；房间固定 `ROOM=1` |
| [`seed-replay.mjs`](../scripts/seed-replay.mjs) | 今日起伏占用曲线（8 学生，目标曲线 `[3,5,6,5,3,2]`） | **直接复用**为时空图/回放铺数据 | 写入今日未来时段 |
| [`smoke-test.mjs`](../scripts/smoke-test.mjs) | 登录/看板/预约/重复拒绝/**8 并发仅 1 成功**/签到/取消/限次/报表/权限 | **并发段可现场跑**，观察 1 成功 | 断言输出，会占额度 |
| [`test-hold.mjs`](../scripts/test-hold.mjs) | 临时锁座 + SSE `seat_hold`/`hold_released` | 参考其 SSE 订阅写法 | 断言脚本 |
| [`test-waitlist.mjs`](../scripts/test-waitlist.mjs) | 候补闭环（占满→候补→释放保留→确认） | 蓝本，重写为演示节奏 | **需干净库** |
| [`test-group.mjs`](../scripts/test-group.mjs) | 组队闭环 + **两组抢重叠相邻座原子回滚** | 蓝本，重写为演示节奏 | **需干净库** |
| [`timeout-test.mjs`](../scripts/timeout-test.mjs) | 超时释放 + 黑名单 | 说明其依赖 `:18081` 短窗口后端 | 见 [../RUN.md](../RUN.md) §超时释放 |
| `sse-test.mjs` / `test-notify.mjs` | SSE 推送 / 通知中心 | 参考 SSE 订阅与通知断言 | — |
| `scripts/report/shots-cdp.mjs` | 免登录注入 `localStorage(satoken/role/userInfo)` | **Playwright 注入登录态的现成范式** | Puppeteer/CDP，非 Playwright |

## 三、演示需要模拟的数据与操作（清单）

按幕次归纳「下层」要产出的东西（与 [04-orchestration.md](04-orchestration.md) 的 cue 点对应）：

1. **看板初始态**：某房间某日多状态座位（空闲/已预约/使用中/不可用），让画面不空。
2. **并发爆发**：N（如 8~10）个不同学生 token 在**同一时刻**抢**同一** `seatId + date + slot`。
3. **锁座联动**：一个学生锁座，产生 `seat_hold`，供另一窗口/看板观察变黄。
4. **候补触发**：占满目标时段 → 一名学生候补 → 释放（取消）→ 触发 60s 保留 + 通知。
5. **组队并发**：两组抢重叠相邻座，恰好一组整体成功。
6. **超时释放**：预约但不签到，等待到期（或短窗口后端）触发 `EXPIRED_RELEASED` + 爽约 + 黑名单。
7. **背景流量（可选）**：低频随机预约/取消，让实时事件流“活”起来。

## 四、新演示驱动脚本规格（伪代码级，本次不落地实现）

> 全部沿用第一节范式。设计原则：**幂等、可参数化（env）、可被主控在 cue 点调用、输出面向观众（少而清晰的中文行）**。

### 4.1 `stage.mjs` — 场景造数（复用 seed-demo 思路，参数化）
- **用途**：一键把某房间某日铺成「有故事」的看板初始态。
- **参数（env）**：`ROOM`（默认 1）、`DATE`（默认明天）、`SLOT`（如 `14:00-16:00`）、`N_RESERVED`/`N_INUSE`/`N_DONE`/`N_CANCELLED`。
- **命中端点**：`board`（找空位）→ `reservations`（造预约）→ `check-in`/`check-out`/`cancel`（造状态）。
- **可见效果**：目标房间该时段出现设定数量的各状态座位。
- **cue**：录制开始前运行一次。

```text
login student1..8
seats = board(ROOM, DATE, SLOT).filter(FREE)
for i in N_RESERVED:  reserve(studentX, seats[i])            // 待签到
for i in N_INUSE:     reserve+check-in(studentY, seats[j])   // 使用中
for i in N_DONE:      reserve+check-in+check-out(...)        // 已完成
for i in N_CANCELLED: reserve+cancel(...)                    // 已取消
print 中文摘要（房间/日期/各状态数量）
```

### 4.2 `race.mjs` — 并发抢座爆发（口播提示点触发）
- **用途**：演示「N 人同时抢一座，仅 1 成功」。
- **参数**：`ROOM`、`DATE`、`SLOT`、`N`（默认 8）、可选 `SEAT_ID`（不给则自动取一个 FREE）。
- **命中端点**：并发 `POST /api/reservations`（同一 `seatId`）。
- **可见效果**：终端打印 `1 成功 / N-1 SEAT_ALREADY_RESERVED`；观察窗口看板该座位变红一次 `seat_reserved`。
- **cue**：口播「现在 8 个人同时抢这一个座」时由主控触发。

```text
tokens = [login student1..N]
seatId = SEAT_ID || firstFree(board(ROOM,DATE,SLOT))
results = await Promise.all(tokens.map(t => reserve(t, seatId, DATE, SLOT)))
ok = results.filter(r => r.code === '0').length
print `并发 ${N}：成功 ${ok}，被拒 ${N-ok}（均 SEAT_ALREADY_RESERVED）`
assert ok === 1
```

### 4.3 `hold-demo.mjs` — 临时锁座联动
- **用途**：配合双窗，展示锁座后另一端变黄 + 倒计时。
- **参数**：`ROOM`、`DATE`、`SLOT`、`AS`（锁座学生，默认 student1）。
- **命中端点**：`POST /api/holds` →（可选）`POST /api/holds/release`。
- **可见效果**：SSE `seat_hold`，另一窗口该座位「🔒 选择中」；释放后 `hold_released` 回 FREE。
- **cue**：通常直接由 Playwright 窗口 A 点座产生；脚本版用于纯 API 联动或无第二个真人操作时。

### 4.4 `waitlist-demo.mjs` — 候补自动补位闭环
- **用途**：占满 → 候补 → 释放保留 60s → 确认。**需干净库**。
- **参数**：`ROOM`、`DATE`、`SLOT`、`HOLDER`（占座者）、`WAITER`（候补者）。
- **命中端点**：占满多座 `reservations` → `POST /api/waitlist`（WAITER）→ `cancel`（释放）→ 轮询 `GET /api/waitlist/me` 至 `OFFERED` →（由候补者窗口点「立即确认预约」或脚本 `accept`）。
- **可见效果**：候补者窗口出现「🔒 席位已保留 60s」+ 铃铛通知（type=`WAITLIST`）。
- **cue**：主控在「候补」幕触发释放；确认动作交给 Playwright（更有观感）。

```text
// 干净库前提
fill = 占满 SLOT 全部空位（多学生 reserve）
join(WAITER, ROOM, DATE, SLOT)                 // status WAITING
cancel(某 holder 的一条)                        // 触发 onSeatReleased
poll waitlist/me(WAITER) until OFFERED (≤60s)  // 保留 60s + 通知
// accept 由 Playwright 窗口点击（或脚本 accept 兜底）
```

### 4.5 `group-demo.mjs` — 组队相邻原子性
- **用途**：两组抢重叠相邻座，恰好一组整体成功、败组回滚。**需干净库**。
- **参数**：`ROOM`、`DATE`、`SLOT`。
- **命中端点**：找同排 4 连续空位 → 并发两次 `POST /api/reservations/group`（成员集合在一个座位上重叠）。
- **可见效果**：终端打印哪组成功；看板胜方两座 `RESERVED`、败方独占座仍 `FREE`（原子回滚）。
- **cue**：口播「两个小组同时抢，只能整组成功」。

### 4.6 `timeout-demo.mjs` — 超时释放 + 黑名单
- **用途**：不签到 → 到期释放 + 爽约 + 黑名单。
- **两种模式**：
  - **压缩模式（推荐录制）**：连临时短窗口后端 `:18081`（`SEATWISE_SIGNIN_WINDOW_MINUTES=0`，见 [../RUN.md](../RUN.md)），预约当前片 → 约 8s 后 `EXPIRED_RELEASED` → 再预约 `USER_IN_BLACKLIST`。脚本 `BASE=http://localhost:18081`。
  - **真实模式**：正常 `:8888`，预约后等 15 分钟（不适合现场，仅说明机制）。
- **命中端点**：`POST /api/reservations`（不签到）→ 等待 → `board` 观察释放 → `GET /api/admin/blacklist` → 再 `reserve` 被拒。
- **可见效果**：看板座位由 `RESERVED` 自动回 `FREE`（`seat_released`）；黑名单页新增记录。

## 五、脚本与 Playwright 的同步时机（cue 约定）

| 脚本 | 由谁触发 | 触发时机 | Playwright 侧观察 |
| --- | --- | --- | --- |
| `stage.mjs` | 主控/人工 | 录制前 | 双窗打开即见铺好的看板 |
| `race.mjs` | 主控（口播提示点） | 说「同时抢」瞬间 | 观察窗看板目标座位变红一次 |
| `waitlist-demo.mjs`（释放段） | 主控 | 候补者已加入后 | 候补者窗铃铛红点 + 「保留 60s」 |
| `group-demo.mjs` | 主控 | 口播「两组同时」 | 看板胜方两座变红、败方座位不变 |
| `timeout-demo.mjs`（:18081） | 主控 | 单独一幕 | 看板座位自动回 FREE |

> 完整幕次编排与主控伪代码见 [04-orchestration.md](04-orchestration.md)。
