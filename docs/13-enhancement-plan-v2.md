# docs/13 · 增强功能计划 V2（对标同类系统）

- **文档目的**：在 [docs/12](12-enhancement-plan.md) 的 ①–⑦ 之上，对标业界/开源同类「图书馆/自习室座位预约系统」的常见能力，规划第二批增强，继续提升**完善度 + 演示效果**。
- **调研来源（同类项目/产品常见功能）**：SpringBoot+Vue 高校自习室系统（公告、扫码签到、信用分、论坛、借还中心）、商用图书馆预约（Setmore/SimplyBook/MIDAS：24h/2h 提醒、门口 QR 签到、利用率分析、收藏/常用）。
- **取舍**：优先**复用现有基础设施**（每用户 SSE、通知中心、定时任务、Redisson、ECharts）、工作量适中、答辩加分、且**不引入正确性风险**的项；论坛/借还中心等偏离自习室主线的重型模块不做。

## 优先级总览
```mermaid
flowchart LR
    A[⑧ 公告中心] --> B[⑨ 预约提醒(开始前/签到开放)]
    B --> C[⑩ 个人自习报告]
    C --> D[⑪ 收藏座位/一键复用]
    D --> E[⑫ 扫码签到(QR)]
```

## ⑧ 公告中心（Announcement）✅ 已实现 2026-07-09
- **价值**：管理员发布系统公告（如「本周五闭馆」），学生端首页横幅 + 公告列表可见；发布即通过每用户 SSE 推送一条站内通知，复用通知中心。
- **落地**：表 `announcement`；`Announcement`/`Mapper`/`Service`/`Controller`；接口 `GET /api/announcements`、`GET/POST /api/admin/announcements`、`PUT/DELETE /api/admin/announcements/{id}`；前端 学生首页横幅 + 管理端「公告管理」页 CRUD；发布可勾选推送（type=`ANNOUNCEMENT`）。测试 `scripts/test-announcement.mjs` 9/9。
- **数据**：新增 `announcement(id, title, content, level[INFO/WARN], active, publisher_id, created_time, updated_time)`。
- **接口**：
  - `GET /api/announcements`（学生/管理员：返回 active 公告，按时间倒序）
  - `POST /api/admin/announcements`（管理员：发布，可选立即推送）
  - `PUT /api/admin/announcements/{id}`（编辑/上下线 active）
  - `DELETE /api/admin/announcements/{id}`（下线/删除）
- **前端**：学生端首页顶部横幅（最高优先级一条）+「公告」入口列表；管理端「公告管理」页 CRUD。
- **推送**：发布时若勾选「同时通知」，对所有学生 `NotificationService.notify(type=ANNOUNCEMENT)`。
- **验收**：`scripts/test-announcement.mjs` —— 发布→学生可见→（勾选推送）学生通知 +1→下线后不可见。

## ⑨ 预约提醒（Reminder）
- **价值**：闭合「我不知道啥时候能签到」的体验缺陷。定时任务在**签到窗口开放时**给用户推一条「可以签到了」；并在**预约开始前 N 分钟**推「即将开始」提醒。
- **实现**：`ScheduledJobs` 增量扫描 `PENDING_SIGN_IN`；用 Redis Set/标志位保证每类提醒每预约只推一次（幂等）。复用 `NotificationService`（type=`REMINDER`）。
- **配置**：`seatwise.remind-before-minutes`（默认 10）。
- **验收**：`scripts/test-reminder.mjs` —— 构造一个「签到窗口已开放」的预约，跑一次提醒逻辑后该用户收到 REMINDER 通知，且重复跑不重复推送。

## ⑩ 个人自习报告（Study Report）✅ 已实现 2026-07-09
- **价值**：学生「个人中心」深度——累计自习时长、完成场次、守约率、连续天数（streak）、近 7 天时长柱状图。纯聚合既有 `reservation`，无需建表，ECharts 呈现，演示友好。
- **落地**：`StudyReportService.report` + `MeController` `GET /api/me/study-report`；前端 `views/student/StudyReport.vue`（KPI 卡 + 近7天柱状图 + 守约环图）+ 菜单/路由。
- **演示数据**：`DataInitializer.seedHistory()` 在无预约时注入最近 7 天「已完成/爽约释放」历史（同时惠及历史回放/排行）。测试 `scripts/test-report-me.mjs` 8/8。
- **接口**：`GET /api/me/study-report` → `{ totalSessions, completedSessions, totalHours, onTimeRate, streakDays, weekly:[{date,hours}] }`。
- **口径**：完成 = 状态 COMPLETED；时长 = Σ(endSlot-startSlot)×slotMinutes/60（或签到到签退实际时长，取预约时段口径更稳）；守约率 = COMPLETED / (COMPLETED + EXPIRED_RELEASED)；streak = 最近连续有完成预约的自然天数。
- **前端**：学生端「自习报告」页（卡片 + 近 7 天柱状图 + 守约率环图）。
- **验收**：`scripts/test-report-me.mjs` —— 造若干完成/爽约预约，断言场次/时长/守约率计算正确。

## ⑪ 收藏座位 / 一键复用（后续，中）
- 收藏常用座位/自习室；「一键按上次时段复用预约」。需 `favorite` 表 + 预约表单预填。排在提醒/报告之后。

## ⑫ 扫码签到（QR，后续，中）
- 预约详情生成签到二维码（编码一次性签到 token）；到馆扫码/门口 QR 完成签到，替代/补充按钮签到。Web 演示以「出示二维码 + 模拟扫码」呈现。放最后。

## 实现约束
- 每落地一项：先在本文件与对应设计文档（`server/02` 库表、`server/03` 接口）补齐，再写代码；每步可运行、可测。
- 通知统一走通知中心；提醒/公告文案写明来由；不新增正确性风险，聚合类只读既有数据。
- 复用既有 SSE / 定时任务 / ECharts，不重复造轮子。

## 验收标准
- 每项均有独立测试脚本通过；前端新增页面同步更新 `client/*` 文档；本文件对应项标注 ✅。
