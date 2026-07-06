# client/10 · 前端实现指南（面向 Coding Agent）

- **文档目的**：给前端 Coding Agent 明确的实现顺序、产物边界与 mock 策略。
- **适用范围**：`client` 编码阶段。
- **读者对象**：Codex/Claude Code/opencode。
- **相关文件**：[../ROADMAP.md](../ROADMAP.md)、[01-page-route-map](01-page-route-map.md)、[07-api-calling-design](07-api-calling-design.md)、[AGENTS.md](AGENTS.md)。

## 关键结论
- 前端按 ROADMAP 阶段推进；后端未就绪的接口先 mock（结构对齐真实响应）。
- 每次实现后回写对应设计文档。

## 一、推荐实现顺序
1. 脚手架：Vite + Router + Pinia + Element Plus + Axios 实例 + 目录结构。
2. userStore + 登录页 + 路由守卫（P1）。
3. api 层封装 + 统一错误处理 + 错误码文案表（贯穿）。
4. roomStore + 筛选页（P1/P2）。
5. SeatGrid/SeatCell + 选座页（P2/P3）。
6. reservationStore + 预约提交 + 我的预约 + 签到/取消（P3/P4）。
7. heatmapStore + SSE 封装 + HeatmapBoard 看板（P5）。
8. adminStore + 基础数据 CRUD + 排布编辑器（P1/P2）。
9. StatsChartCard + 报表页（P6）。
10. 【MVP+】scoreStore + 排行榜页（P7）；nearbyRoomStore + 附近空位页（P8）。

## 二、每阶段生成哪些文件
| 阶段 | 主要文件 |
| --- | --- |
| P1 | `api/auth.ts`,`stores/user.ts`,`views/Login.vue`,`router/index.ts` |
| P2 | `api/room.ts`,`stores/room.ts`,`components/*Selector.vue`,`RoomCard.vue`,`SeatGrid.vue`,`SeatCell.vue` |
| P3 | `api/reservation.ts`,`stores/reservation.ts`,`views/student/Seats.vue`,`ReservationTimePicker.vue`,`ReservationConfirmDialog.vue` |
| P4 | `views/student/Reservations.vue`,`CheckIn`,`BlacklistNotice.vue` |
| P5 | `sse/boardStream.ts`,`stores/heatmap.ts`,`HeatmapBoard.vue` |
| P6 | `api/report.ts`,`StatsChartCard.vue`,`views/admin/Reports.vue` |
| P7 | `api/score.ts`,`stores/score.ts`,`ScoreRankingTable.vue` |
| P8 | `api/nearby.ts`,`stores/nearbyRoom.ts`,`NearbyAvailableRoomList.vue` |

## 三、哪些组件先 mock
- SeatGrid 数据、report 数据、score/nearby 数据可先 mock。
- SSE 可用本地定时器模拟 `seat_*` 事件联调。

## 四、哪些功能依赖后端
- 预约提交结果、并发失败、黑名单判定、签到窗口、真实 SSE 事件——必须等后端，禁止前端伪造成功。

## 五、MVP 必做页面
登录、筛选、选座、我的预约、签到、黑名单提示、管理端基础数据/排布/看板/报表。

## 六、可后续做页面
排行榜、附近空位、积分记录、通知中心、校园地图。

## 七、文件粒度
- 不要一次性生成巨型页面；按组件拆分，单文件聚焦单职责。
- 每次提交范围小、可运行。

## 八、每次实现后更新文档
- 新页面→[01](01-page-route-map.md)；新组件→[05](05-component-design.md)；新接口→[07](07-api-calling-design.md)；新 store→[06](06-state-management.md)。

## 实现约束
- 遵守 [AGENTS.md](AGENTS.md)：不判定最终结果、座位状态仅快照+SSE、API 集中封装。

## 验收标准
- 每阶段产物对照 [09](09-client-test-and-acceptance.md) 勾选项通过。

## 给 AI Coding Agent 的提示
先确认当前 P 阶段，只做该阶段文件；mock 响应结构必须与 [../server/03-api-design.md](../server/03-api-design.md) 一致，切真实零改动。
