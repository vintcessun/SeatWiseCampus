# pre/scripts · 双层演示框架（可运行）

> 上层 **Playwright**（关键用户操作，Edge 有头浏览器）+ 下层 **API 脚本**（多用户并发/背景/时间压缩），
> 由 **主控 run.mjs** 按幕次编排。设计说明见 [../00-overview.md](../00-overview.md) ~ [../04-orchestration.md](../04-orchestration.md)。

## 0. 环境前置
- 已启动 Docker 全栈：`docker compose up -d`，浏览器入口 `http://localhost:8888`（脚本默认 `BASE`）。
- Node 18+（内置 fetch）。本目录 Playwright 用 **playwright-core + 系统 Edge**（`channel: msedge`），**无需下载 Chromium**。
- 安装依赖（仅 playwright-core，秒级）：
  ```bash
  cd pre/scripts
  npm install
  ```
- 演示账号：`admin/admin123`、`student1..8/123456`。

## 1. 一条命令跑完整演示
```bash
node run.mjs            # 起两个有头 Edge 窗口，按幕次串联（上层 PW + 下层 API）
DWELL=6000 SLOWMO=500 node run.mjs   # 放慢节奏，便于讲解/录制
$env:DWELL=6000; $env:SLOWMO=500; node run.mjs # windows用户在ps运行
```
> 建议在**干净库**上跑完整流程：`docker compose down -v && docker compose up -d`，就绪后再 `node run.mjs`。
> 每个学生每日限 3 次预约，反复整轮跑会触发限次；重录前重置库最稳。

## 2. 单独跑某一幕（便于分幕录制）
```bash
node run.mjs <幕名>
```
| 幕名 | 说明 | 层 |
| --- | --- | --- |
| `login` | 两窗真实表单登录 | PW |
| `reserve` | A 选座预约 → B 端看板秒级变红（SSE） | PW×2 |
| `hold` | A 点座临时锁 → B 端变黄"选择中" | PW×2 |
| `board` | A 预约 → B 管理端看板+事件流同步 | PW×2 |
| `race` | 8 人并发抢一座，仅 1 成功；看板观察 | API+PW |
| `ai` | AI 助手 → 可解释 Top-3 推荐 | PW |
| `group` | 组队相邻预约（选连续座+分配成员） | PW |
| `spacetime` | 时空座位图 + 时间轴播放 | PW |
| `replay` | 管理端历史回放（播放/仪表盘） | PW |
| `tags` | 座位标签右键编辑 + 保存布局持久化 | PW |
| `map` | 楼栋地图选点保存坐标 | PW |
| `pomodoro` | 番茄钟 | PW |
| `waitlistAccept` | 「我的候补」点「立即确认预约」（需先造 OFFERED） | PW |

## 3. 单独跑下层 API 脚本（多用户/并发/背景）
```bash
node stage.mjs            # 造看板初始态（多状态座位）
node race.mjs             # N 人并发抢一座（默认 8）
node hold-demo.mjs        # 临时锁座（SSE seat_hold）
node waitlist-demo.mjs    # 候补自动补位闭环（reserve→join→cancel→OFFERED）
node group-demo.mjs       # 组队相邻 + 两组并发原子回滚
node timeout-demo.mjs     # 超时释放+黑名单（需 :18081 短窗口后端，见脚本顶部注释）
```
常用参数（环境变量）：`BASE`、`ROOM`、`DATE`、`SLOT`（如 `16:00-18:00`）、`N`、`ACCEPT`、`HOLDER`、`WAITER`。

## 4. 复用自 scripts/ 的脚本（已拷贝，自包含，不跨目录引用）
```bash
node seed-demo.mjs        # 明天 A301 多状态预约
node seed-replay.mjs      # 今日起伏占用曲线（时空图/回放用）
node smoke-test.mjs       # 核心闭环 + 8 并发（断言版）
```

## 5. 典型「候补自动补位」联动录制
```bash
node waitlist-demo.mjs        # 造出 OFFERED（不自动确认），60s 内→
node run.mjs waitlistAccept   # B 前端点「立即确认预约」
```

## 6. 常用调参（环境变量）
| 变量 | 作用 | 默认 |
| --- | --- | --- |
| `HEADLESS` | `1`=无头（自检用），`0`=有头（录制用） | `0` |
| `SLOWMO` | Playwright 动作放慢毫秒 | 有头 350 |
| `DWELL` | 每幕结束停留毫秒（讲解/录制） | 3500 |
| `BASE` | 后端/前端入口 | `http://localhost:8888` |
| `ROOM` | 房间 id | 首个房间 |
| `PW_CHANNEL` | 浏览器通道（`msedge`/`chrome`） | `msedge` |

## 7. 目录内文件
- `lib.mjs` — 下层通用 API 库（fetch + satoken + 领域封装）。
- `pwlib.mjs` — 上层 Playwright 库（Edge 启动、注入登录态、座位网格操作/观察）。
- `pw-scenes.mjs` — 各可见幕次（`SCENES`）。
- `run.mjs` — 主控编排。
- `stage/race/hold-demo/waitlist-demo/group-demo/timeout-demo.mjs` — 下层演示脚本。
- `seed-demo/seed-replay/smoke-test.mjs` — 自 `scripts/` 拷贝复用。

## 8. 说明与边界
- **并发抢座 / 组队并发原子性**：由 API 脚本 `Promise.all` 产生真实并发，Playwright 逐页点击无法等价。
- **超时释放/黑名单**：正常后端签到窗口 15 分钟，无法现场压缩；`timeout-demo.mjs` 需连 `:18081` 短窗口后端（命令见脚本顶部与 [../../RUN.md](../../RUN.md)）。
- **登录**：功能幕用 `localStorage` 注入（与语言无关、规避验证码）；`login` 幕用真实表单演示。
- **干净库**：候补/组队/并发的"恰好一个成功"类效果在干净库最稳；整轮演示前 `docker compose down -v && up -d`。
