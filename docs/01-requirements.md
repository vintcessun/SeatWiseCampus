# docs/01 · 需求规格说明

- **文档目的**：完整定义 SeatWise Campus 的功能、非功能、权限、数据、并发、实时、报表及扩展需求。
- **适用范围**：全项目需求基线。
- **读者对象**：产品/开发/测试/Agent。
- **相关文件**：[docs/02-system-architecture.md](02-system-architecture.md)、[docs/03-core-business-flows.md](03-core-business-flows.md)、[docs/04-mvp-scope.md](04-mvp-scope.md)、[docs/05-extension-design.md](05-extension-design.md)、[docs/14-known-issues-v3.md](14-known-issues-v3.md)。

## 关键结论
- 系统围绕**学生限时预约**与**管理员基础数据+看板报表**两条主线。
- 强约束是**黑名单**与**并发唯一性**；积分是**弱激励**，不参与权限。
- 实时性通过**快照+SSE**满足；报表通过**聚合表**满足。

## 一、背景与目标
见 [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md)。目标：让学生方便找到并锁定空位，让管理员掌握使用情况，用规则保障公平。

## 二、用户角色
STUDENT（含受限黑名单态）、ADMIN、SYSTEM（定时任务）。权限详见 [server/04-auth-rbac.md](../server/04-auth-rbac.md)。

## 三、功能需求
### FR-1 自习室管理【MVP】
| 编号 | 需求 |
| --- | --- |
| FR-1.1 | 管理员录入校区 |
| FR-1.2 | 管理员录入楼栋 |
| FR-1.3 | 管理员录入楼层 |
| FR-1.4 | 管理员录入自习室房间 |
| FR-1.5 | 配置自习室开放时间 |
| FR-1.6 | 配置座位排布（行列网格 + cell_type） |
| FR-1.7 | 座位启用/禁用 |
| FR-1.8 | **（待确认）** 管理员端支持自定义座位行列个数（详见 [docs/08](08-known-issues.md) §P1） |
| FR-1.9 | **（待确认）** 座位网格增加讲台/门标记元素（详见 [docs/08](08-known-issues.md) §P1） |
| FR-1.A | **（待确认）** 自定义课桌间距；课桌形态默认单人桌，暂不支持其他形态（详见 [docs/08](08-known-issues.md) §P1） |
| FR-1.B | **（待确认）** 管理员看板切换日期查看未来座位预约情况（详见 [docs/08](08-known-issues.md) §P1） |
| FR-1.C | **（待确认）** 管理员看板红/黄/绿/灰色语义提示（详见 [docs/08](08-known-issues.md) §P1） |
| FR-1.D | **（待确认）** 自习室需支持「删除」与「暂时关闭」功能，关闭时联动公告通知已预约学生（详见 [docs/14](14-known-issues-v3.md) §R3 §R4 §R10） |
| FR-1.E | **（待确认）** 座位排布需支持每个单元格独立设置类型（多过道/不规则排布），详见 [docs/14](14-known-issues-v3.md) §R5 |
| FR-1.F | **（待确认）** 新增/编辑楼栋、校区的能力（详见 [docs/14](14-known-issues-v3.md) §R3） |

### 已知缺项与安全增强
已知问题清单已集中迁移至 [docs/08-known-issues.md](08-known-issues.md)。此章节不再维护，详细信息以 `docs/08` 为准。

### FR-2 学生预约【MVP】
| 编号 | 需求 |
| --- | --- |
| FR-2.1 | 学生登录 |
| FR-2.2 | 按校区/楼栋/楼层/自习室筛选 |
| FR-2.3 | 查看自习室空位 |
| FR-2.4 | 选择日期与预约起止时间（30 分钟片） |
| FR-2.5 | 座位网格中自选空闲座位 |
| FR-2.6 | 提交预约 |
| FR-2.7 | 限制单日预约次数 |
| FR-2.8 | 限制黑名单用户预约 |
| FR-2.9 | 查看本人预约记录 |

### FR-3 座位管控【MVP】
| 编号 | 需求 |
| --- | --- |
| FR-3.1 | 预约成功进入待签到（PENDING_SIGN_IN） |
| FR-3.2 | 预约开始后指定时间（默认 15 分钟）内签到 |
| FR-3.2b | 学生可主动签退（IN_USE→COMPLETED），释放座位 |
| FR-3.2c | 未主动签退时，预约结束时间到达后自动完成并释放座位（座位不永久停留在 USING） |
| FR-3.3 | 超时未签到自动释放（EXPIRED_RELEASED） |
| FR-3.4 | 学生可主动取消 |
| FR-3.5 | 临时取消不算爽约 |
| FR-3.6 | 超时未签到算爽约（no_show_count+1） |
| FR-3.7 | 爽约达阈值进入黑名单 |
| FR-3.8 | 黑名单期内不能预约 |

### FR-4 实时看板【MVP】
座位热力图；状态含 FREE/RESERVED/USING/DISABLED 及“当前用户已预约”高亮；多客户端实时同步；采用初始化快照 + SSE 增量推送。

### FR-5 数据报表【MVP】
日均使用率、热门时段、取消率、爽约率、利用率排行；支持按校区/楼栋/自习室筛选。

### FR-6 积分排名【MVP+】
守约加分、爽约扣分、排行榜。规则见 [docs/05](05-extension-design.md) 与 [server/09](../server/09-score-ranking-design.md)。

### FR-7 最近空位推荐【MVP+】

> **已知缺陷**：附近空位功能未结合真实定位，缺少管理员位置管理（经纬度配置/地图选点）能力（详见 [docs/14-known-issues-v3.md](14-known-issues-v3.md) §R11）。
按距离 + 空位数 + 开放状态推荐自习室。见 [server/10](../server/10-nearest-available-room-design.md)。

### FR-8 AI 推荐 / 通知提醒【扩展】
仅设计接口与文档，不要求实现。

> **已知问题**：通知中心未正确区分通知类型，不同类型（公告/提醒/积分）均显示为 announcement（详见 [docs/14-known-issues-v3.md](14-known-issues-v3.md) §R9）。

### FR-8b 预约规则提示【改进】
学生端缺少预约须知/规则说明（签到窗口、扣分规则、黑名单规则），需增加静态或交互式提示（详见 [docs/14-known-issues-v3.md](14-known-issues-v3.md) §R8）。

### FR-9 自助注册【扩展】
自助注册功能，当前不纳入 MVP，由管理员预置用户，后续如需开放自助注册再实现。预留设计见 [docs/05](05-extension-design.md) §H。

## 四、非功能需求
| 类别 | 要求 |
| --- | --- |
| 正确性 | 并发下座位时间片唯一，绝不双占 |
| 实时性 | 座位状态变更 → 其他客户端秒级可见 |
| 性能 | 报表读聚合表；看板读缓存+快照；避免全表实时扫描 |
| 可用性 | SSE 断线自动重连；依赖故障不导致双占（唯一索引兜底） |
| 可维护性 | 分层清晰，文档与代码同步 |
| 可部署性 | Docker Compose 一键起 mysql/redis/backend |

## 五、权限需求
学生仅操作本人预约；仅管理员维护基础数据；黑名单用户可登录、可查看历史，不可预约。矩阵见 [server/04](../server/04-auth-rbac.md)。

## 六、数据需求
核心实体：User/Campus/Building/StudyRoom/Seat/Reservation/ReservationSlot/BlacklistRecord/ScoreRecord/RoomDailyStats/OperationLog；扩展：Notification/UserPreference/RoomLocation。详见 [server/02](../server/02-database-schema.md)。

## 七、并发需求
- 多人同座同片：仅一人成功，其余得到 `SEAT_ALREADY_RESERVED`。
- 手段：Redisson 锁 + `reservation_slot` 唯一索引兜底；禁止仅查后插或仅 Redis。详见 [server/05](../server/05-reservation-concurrency-control.md)。

## 八、实时性需求
看板初始化拉快照，之后仅推增量事件；心跳保活；断线重连后重取快照。详见 [server/07](../server/07-sse-realtime-board.md)。

## 九、报表需求
统计口径固定，数据来自 `room_daily_stats` 聚合表，定时任务生成；报表接口返回结构见 [server/08](../server/08-statistics-reporting.md)。

## 十、积分排名需求
积分仅激励；变更必须留流水；排行榜按周/月。与黑名单解耦。见 [server/09](../server/09-score-ranking-design.md)。

## 十一、最近空位推荐需求
MVP 手动选当前位置 + 配置坐标；增强用浏览器定位；不返回无空位自习室。见 [server/10](../server/10-nearest-available-room-design.md)。

## 十二、非目标范围
真实 GPS/室内定位强依赖、AI 模型落地、真实推送通道、支付/门禁、跨校互通、原生 App/小程序（后续）。

## 实现约束
所有功能带分级标签；未排期扩展不得提前实现；正确性优先于性能与体验。

## 验收标准
逐条 FR 可在 [docs/07](07-acceptance-checklist.md) 找到对应验收项；并发/超时/黑名单有专项验收。

## 给 AI Coding Agent 的提示
按 FR 编号对应 ROADMAP 的 P 阶段实现；只做当前阶段的 FR，跨阶段前先确认。
