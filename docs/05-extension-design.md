# docs/05 · 扩展功能设计

- **文档目的**：集中设计附加功能，明确业务价值、是否纳入 MVP、数据库/接口/前端预留、优先级与可砍方案。
- **适用范围**：积分排名、附近空位、自助注册、AI 推荐、通知、校园地图、小程序、规则配置中心。
- **读者对象**：架构/前后端/Agent。
- **相关文件**：[server/09](../server/09-score-ranking-design.md)、[server/10](../server/10-nearest-available-room-design.md)、[docs/04](04-mvp-scope.md)、[docs/14-known-issues-v3.md](14-known-issues-v3.md)。

## 关键结论
- 扩展功能**预留优先、实现延后**：数据库与接口先留位，业务逻辑按阶段推进。
- 积分与附近空位属 **MVP+**；自助注册 / AI / 通知 / 地图 / 小程序 / 规则中心属 **后续**。

---

## A. 积分排名制【MVP+，P7】
- **业务价值**：激励按时签到/离开、合理用座，可产出排行榜提升活跃度。
- **是否纳入 MVP**：否，MVP+。
- **积分规则**：正常签退 +2；开始前 >30 分钟取消不扣；开始前 30 分钟内取消 -1；超时未签到 -3；使用后未签退可选扣分。仅激励，不做权限。
- **数据库预留**：`sys_user.credit_score`；`score_record`（流水）。见 [server/02](../server/02-database-schema.md)。
- **接口预留**：`GET /api/scores/me`、`GET /api/scores/ranking?period=week|month`、`GET /api/admin/scores`。
- **前端预留**：`/student/ranking` 排行榜页、学生端积分展示、`/admin/scores` 记录页、`ScoreRankingTable`、`scoreStore`。
- **优先级**：MVP 之后第一优先。
- **可砍方案**：先硬编码规则，不做管理端规则配置；排行榜可先只做周榜。

## B. 自动定位最近的有空位自习室【MVP+，P8】
- **业务价值**：多校区/多楼栋下快速找到就近空位，减少空跑。
- **是否纳入 MVP**：否，MVP+。
- **MVP 简化方案**：不接真实 GPS；管理员为 campus/building/study_room 配置经纬度或平面坐标；学生手动选当前校区/楼栋；按距离 + 空位数 + 开放状态推荐。
- **增强方案**：浏览器 Geolocation 获取位置；定位失败回退手动选择；排序纳入未来 30 分钟可约、历史热门、用户偏好。
- **数据库预留**：`campus/building/study_room` 增 `latitude/longitude/map_x/map_y`。
- **接口预留**：`GET /api/rooms/availability-summary`、`GET /api/rooms/nearest-available`。
- **前端预留**：`/student/nearby` 页、`NearbyAvailableRoomList`、`nearbyRoomStore`、后续校园地图组件扩展点。
- **优先级**：MVP+ 第二优先。
- **可砍方案**：先手动选位置 + 简单排序（同楼栋 > 同校区最近 > 空位更多），不接浏览器定位与地图。

## C. 自助注册【后续】
> **已知缺陷**：注册入口作为登录页右下角角标，位置不显眼。缺少图形验证码（CAPTCHA），存在自动化注册风险（详见 [docs/14-known-issues-v3.md](14-known-issues-v3.md) §R1）。
- **业务价值**：学生/管理员可自助注册账号，无需管理员预置。
- **是否纳入 MVP**：否。当前用户由管理员预置或种子数据初始化。
- **数据库预留**：`sys_user` 表已有 username/password/role 等字段，无需额外表。
- **接口预留**：`POST /api/auth/register`
  - 请求：`{ "username": "newuser", "password": "***", "realName": "张三", "role": "STUDENT" }`
  - 校验：用户名唯一、密码强度、必填字段；后端存储密码前需加密（具体算法待确认）。
- **前端预留**：`/register` 路由 + `RegisterForm` 组件（用户名/密码/确认密码/姓名/角色选择）。
- **优先级**：后续中优先，仅次于 MVP+。
- **可砍方案**：保持管理员预置方案，不做自助注册。

## D. AI 推荐【后续，P9】
- **业务价值**：按历史使用率/常用楼栋/常用时段推荐更可能有空位的自习室与座位，并给错峰建议。
- **是否纳入 MVP**：否。
- **数据库预留**：复用 `room_daily_stats`、`user_preference`（可选）。
- **接口预留**：`GET /api/ai/recommend/rooms`、`GET /api/ai/recommend/seats`、`GET /api/ai/recommend/offpeak`（先返回规则/占位结果）。
- **前端预留**：在附近空位页或首页嵌入"推荐"卡片位。
- **优先级**：最低。
- **可砍方案**：只设计接口与文档，不实现模型；用统计规则替代。

## E. 通知提醒【后续，P9】
- **业务价值**：开始前提醒、超时前提醒、黑名单解禁提醒，降低爽约。
- **是否纳入 MVP**：否。
- **数据库预留**：`notification` 表（user_id/type/title/content/status/created_time/read_time）。
- **接口预留**：`GET /api/notifications`、`POST /api/notifications/{id}/read`。
- **前端预留**：通知中心页面 + 站内红点。
- **优先级**：低。
- **可砍方案**：只做站内通知，不接真实短信/推送通道。

## F. 校园地图【后续】
- **业务价值**：在地图上直观展示自习室位置与空位。
- **数据库预留**：坐标字段（见 B）。
- **接口预留**：复用 availability-summary。
- **前端预留**：地图组件扩展点（可用平面图 + 坐标点）。
- **可砍方案**：先用列表替代地图。

## G. 微信小程序【后续】
- **业务价值**：移动端便捷预约。
- **预留**：后端接口保持与 Web 一致（REST），小程序复用 API。
- **可砍方案**：整体后置，不影响 Web。

## G2. 子管理员账号【后续】
- **业务价值**：支持多级管理员，各楼层/区域管理员拥有有限权限。
- **角色设计**：新增 ADMIN_SUB（或类似），仅可管理自习室/查看报表，不可管理其他管理员。
- **相关缺陷**：详见 [docs/14-known-issues-v3.md](14-known-issues-v3.md) §R6。

## H. 预约规则配置中心【后续】
- **业务价值**：管理员可配置单日次数、签到窗口、黑名单阈值、积分规则。
- **数据库预留**：`sys_config` 或规则表（后续）。
- **接口预留**：`/api/admin/config/**`。
- **可砍方案**：MVP 全部硬编码为常量，集中在一个配置类，便于后续外置。

## 扩展优先级总览
```mermaid
flowchart LR
    A[积分排名] --> B[附近空位] --> C[自助注册] --> D[AI推荐] --> E[通知] --> F[校园地图] --> G[小程序] --> H[规则配置中心]
```

## 实现约束
- 预留字段可随 MVP 一起建表，但**扩展业务逻辑必须延后到对应阶段**。
- 扩展接口先定义契约（路径/参数/返回），可返回占位数据。

## 验收标准
- 每个扩展在 [docs/07](07-acceptance-checklist.md) 有"扩展功能验收"项（按已实现范围核对）。

## 给 AI Coding Agent 的提示
不要因为"表里有字段"就顺手实现扩展逻辑；严格按 ROADMAP 阶段推进，跨阶段先确认。
