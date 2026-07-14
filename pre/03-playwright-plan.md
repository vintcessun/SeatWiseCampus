# pre/03 · Playwright 操作方案（对应 Q3）

- **文档目的**：说明 Playwright 用两个有头浏览器驱动哪些关键用户操作、如何登录、窗口角色如何随幕次切换，并划清 Playwright 的能力边界。
- **适用范围**：演示上层「可见叙事」的自动化。
- **读者对象**：编写 Playwright 脚本的 Agent、演示者。
- **相关文件**：[00-overview.md](00-overview.md)、[01-feature-catalog.md](01-feature-catalog.md)、[02-api-simulation.md](02-api-simulation.md)、[04-orchestration.md](04-orchestration.md)、[../client/src/router/index.js](../client/src/router/index.js)、[../client/src/stores/user.js](../client/src/stores/user.js)。

---

## 关键结论

- **UI 可见动作几乎都能用 Playwright 完成**：登录、选座预约闭环、锁座、候补确认、组队闭环、推荐、时空图/回放、标签编辑、地图选点、番茄钟，以及**双窗实时看板同步**。
- **三件事 Playwright 做不了/不适合**，须交回 API 脚本或特殊后端：① N 人同一时刻抢同一座的真并发；② 超时释放的时间压缩；③ 注册验证码 OCR。
- **窗口角色随幕次切换**：默认两学生（主操作 + 观察联动），演示管理端时换成「学生 + 管理员」。
- Playwright **当前不是项目依赖**（`scripts/report` 用 Puppeteer/CDP），需新增 `@playwright/test` 或 `playwright`。

---

## 一、前置与工程

- **新增依赖**：`playwright`（或 `@playwright/test`）；`npx playwright install chromium`。放在独立目录（如 `scripts/demo/`），不污染 `client`/`server`。
- **有头 + 慢放**：`chromium.launch({ headless:false, slowMo: 300 })`，窗口尺寸约 `1440x1000`，便于录制。
- **多窗口**：用**两个独立 `browserContext`**（各自独立 `localStorage`/登录态），每个 `newPage()` 一个可见窗口；需要时开第 3 个 context。
- **目标地址**：`http://localhost:8888`（history 路由，无 `#`）。

## 二、登录策略（两种，优先前者）

1. **快捷登录（可见、可靠、推荐用于「登录」幕）**：`/login` 页有按钮「管理员 admin」「学生 张三」「学生 李四」，点击即自动提交。
2. **注入 localStorage（跳过登录 UI，用于纯功能幕）**：先 API 登录拿 `{token,role,userInfo}`，再对该 context：

```js
// 参考 scripts/report/shots-cdp.mjs 的注入范式
await page.goto('http://localhost:8888/login')
await page.evaluate(({t,r,u}) => {
  localStorage.setItem('satoken', t)
  localStorage.setItem('role', r)
  localStorage.setItem('userInfo', JSON.stringify(u))
}, { t: token, r: role, u: userInfo })
await page.goto('http://localhost:8888/student/rooms')   // 路由守卫据 satoken/role 放行
```

> 路由守卫（`router/index.js`）读 `localStorage.satoken` + `role`；无 token → `/login`，角色不符自动在 student/admin 间跳转。

## 三、窗口角色（随幕次切换）

| 幕次 | 窗口 A | 窗口 B | 观察点 |
| --- | --- | --- | --- |
| 登录 | student1（快捷登录） | student2（快捷登录） | 各自进入学生端 |
| 预约闭环 | student1 选座/签到 | student2 看板观察 | A 预约 → B 端座位变红 |
| 临时锁座 | student1 点座锁定 | student2 同房间 | B 端座位变黄「🔒 选择中」+ 倒计时 |
| 实时看板同步 | student1 操作 | **admin** `/admin/rooms/:id/board` | 管理端看板 + 实时事件流秒级同步 |
| 候补补位 | student2 候补者 | student1 或脚本占座/释放 | A/脚本释放 → B 收「保留 60s」+ 确认 |
| 时空图/回放/推荐/标签/地图/番茄钟 | 对应角色单窗为主 | 另一窗待命 | 见各幕 |

> 每一幕执行前在脚本里明确「本幕窗口配置」。

## 四、能力边界表

| 功能 | Playwright 可独立完成？ | 需 API 脚本/特殊后端配合？ | 原因 |
| --- | --- | --- | --- |
| 登录（快捷/注入） | ✅ | 否 | 有快捷按钮；验证码仅注册需要 |
| 注册 | ⚠️ | 是（或跳过） | 验证码图片无法 OCR，用快捷登录/注入替代 |
| 选座→锁座→预约→签到→签退→取消 | ✅ | 否 | 全是页面操作 |
| 实时看板双端同步 | ✅（观察） | 变化可由 PW 或 API 触发 | 两窗 SSE 观察即可 |
| 临时锁座联动 | ✅ | 否（或脚本联动） | A 点座、B 观察 |
| 候补自动补位 | ⚠️ 确认可，占满/释放建议脚本 | 是 | 占满全时段 + 精确释放用脚本更快更稳；**需干净库** |
| 组队相邻预约（功能闭环） | ✅ | 否 | 选连续座 + 分配成员 |
| 组队并发原子性 | ❌ | 是 | 需两组**同时**提交，逐页点击非并发 |
| 并发抢座（N 人抢一座） | ❌ | 是 | 需真正并发 `Promise.all` |
| 超时释放 + 黑名单 | ❌ | 是（`:18081` 短窗口后端） | 无法压缩服务端定时器 |
| 智能推荐 / 替代方案 | ✅ | 否 | 点 🤖 发送、点卡片 |
| 时空座位图 + 时间轴播放 | ✅ | 数据可先 `seed-replay` | 拖 slider / 播放 |
| 历史回放 | ✅ | 同上 | 播放/倍速/仪表盘 |
| 座位标签编辑 + 持久化 | ✅ | 否 | 右键勾选 + 保存布局 + 刷新验证 |
| 地图选点 | ✅ | 否 | Leaflet 点选 + 保存 |
| 番茄钟 | ✅ | 否 | 纯前端 |
| 报表/积分/概览/个人报告 | ✅ | 数据可先造 | 直接进页面 |
| 通知中心红点 | ✅（观察） | 是（事件由脚本触发） | 候补/黑名单/积分事件点亮红点 |

**结论**：Playwright 覆盖所有「可见叙事」；把 ❌/⚠️ 的并发、超时、验证码交给 API 脚本 / `:18081` 后端 / 注入登录。

## 五、逐功能 Playwright 步骤（选择器基于已核实文案）

> 选择器优先用可见文案（`getByRole('button',{name})` / `getByText`）；下面文案均来自实际组件。时间选择须选**当天未来**时段，否则前端 `ElMessage.warning` 拦截提交。

### 5.1 登录
- `page.goto('/login')` → 点「学生 张三」/「学生 李四」快捷按钮 → 自动进入 `/student/home`。

### 5.2 选座 → 锁座 → 预约（窗口 A） + 看板观察（窗口 B）
- A：`/student/rooms` → 卡片点「进入选座」进入 `/student/rooms/:roomId/seats`。
- A：选日期 `el-date-picker`、开始/结束 `el-time-select`（如 14:00–16:00）。
- A：点一个**绿色 FREE 座位** → 触发 `holdApi.hold`（锁座 90s）→ 弹窗「确认预约」（内含倒计时）→ 点「确认预约」→ 彩带 + 座位变红。
- B：同房间 `/admin/rooms/:roomId/board` 或学生 Seats，**观察该座位秒级变红**（SSE `seat_reserved`）。

### 5.3 签到 / 签退 / 取消（窗口 A）
- `/student/reservations` → 目标行：状态 `PENDING_SIGN_IN` 点「签到」；`IN_USE` 点「签退」；点「取消」确认。
- 注：签到按钮在签到窗口外禁用（tooltip「未到签到时间」）——演示签到需预约近端时段。

### 5.4 临时锁座联动（A 锁 / B 看黄）
- A：Seats 页点座（不确认，保持锁定倒计时对话框开着）。
- B：同房间看板/Seats，目标座位显示黄色「🔒 选择中」+ 倒计时（`seat_hold`）。
- A：关闭对话框（未确认）→ 座位回 FREE（`hold_released`）。

### 5.5 候补自动补位（B 候补者 + 脚本占座/释放）
- 前置（脚本）：`waitlist-demo.mjs` 占满目标时段。
- B：Seats 页显示「全部占满，加入候补队列」→ 点它加入 → 进 `/student/waitlist` 见 `WAITING`。
- 触发（脚本/A）：释放一座。
- B：`/student/waitlist` 行变「🔒 席位已保留 60s」+ 顶部铃铛红点 → 点「立即确认预约」→ 跳 `/student/reservations` 见新预约。

### 5.6 组队相邻预约（窗口 A，功能闭环）
- A：Seats 页开「组队相邻预约」`el-switch` → 网格选同排连续座 → 分配成员（student2/3…）→ 提交 → 成员各自名下出现预约。
- （并发原子性由 `group-demo.mjs` 演，非 Playwright。）

### 5.7 智能推荐（窗口 A）
- A：任意学生页点右下角 🤖 → 点示例词「下午2点安静靠窗坐两小时」或输入 → 点「发送」→ 展示 Top-3 卡片（排名/房间/标签/理由/`source` 标签）→ 点某卡「前往预约 →」跳到该座 Seats 页。
- 关联：制造抢座失败后，Seats 页弹「🤖 座位刚被抢走，为你找到替代方案」→ 一键改约。

### 5.8 时空座位图 + 时间轴（窗口 A）
- 前置（脚本）：`seed-replay.mjs` 铺今日曲线。
- A：`/student/spacetime` → 选房间/日期 → 拖 `el-slider` 时间轴或点「播放一天」→ 座位按连续可用时长发光 → 点发光座弹「预约 HH:MM-HH:MM →」。

### 5.9 历史回放（管理端）
- admin：`/admin/rooms/:roomId/board` 点「历史回放」→ `/admin/rooms/:roomId/replay` → 点播放 / 切倍速 `el-segmented`（0.5x/1x/2x）→ 看利用率仪表盘 + 曲线定位最拥挤时刻。

### 5.10 座位标签编辑 + 持久化（管理端）
- admin：`/admin/rooms/:roomId/layout` → **右键**某 SEAT 格 → 弹「座位属性」勾选（靠窗/有插座/安静区/讨论区/靠门）→ 点「保存布局」→ **刷新页面**验证徽标仍在（持久化）。

### 5.11 地图选点（管理端）
- admin：`/admin/locations` → 某楼栋行点「地图选点」→ 弹窗 Leaflet 地图上点击/拖标记 → 点「确认坐标」→ 点「保存坐标」（`PUT /api/buildings/{id}/location`）。

### 5.12 番茄钟（窗口 A）
- A：`/student/pomodoro` → 点「开始」→ 环形进度走动 →（说明本地计时）点「跳过」演示完成彩带/提示音；切换模式 `el-segmented`（专注/短休/长休）。

### 5.13 报表 / 积分 / 概览 / 个人报告 / 通知
- admin：`/admin/reports`（ECharts 图）、`/admin/dashboard`（概览卡片）。
- 学生：`/student/ranking`（排行）、`/student/home`（概览）、`/student/report`（个人报告近 7 天图）。
- 通知：顶部铃铛；红点由候补/黑名单/积分事件（API 脚本触发）点亮，点开抽屉看「写明原因」的通知。

## 六、录制稳定性建议

- 用 `page.waitForSelector` / `expect(...).toBeVisible()` 等待元素，勿用固定 `sleep` 抢跑（SSE 变色可 `waitForFunction` 检测 class/颜色变化）。
- 关键幕前 `page.bringToFront()` 让目标窗口置顶入镜。
- 双窗同步幕：两个 context 都订阅同一房间看板，操作前先确认两窗都「实时连接中」。
- 失败恢复：重置库 `docker compose down -v && up -d` + 重跑 `stage.mjs`，再重开双窗。
