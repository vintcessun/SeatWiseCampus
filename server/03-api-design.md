# server/03 · REST API 设计

- **文档目的**：定义 REST 接口契约、请求/响应示例与业务错误码。
- **适用范围**：`/api/**` 与 SSE 端点。
- **读者对象**：前后端/Agent。
- **相关文件**：[04-auth-rbac](04-auth-rbac.md)、[05-reservation-concurrency-control](05-reservation-concurrency-control.md)、[07-sse-realtime-board](07-sse-realtime-board.md)、[../GLOSSARY.md](../GLOSSARY.md)。

## 关键结论
- 统一响应 `{code,message,data,traceId}`；`code=0` 成功。
- 资源用复数名词，动作用子路径；权限见每接口“权限”列。

## 一、统一响应
```json
{ "code": 0, "message": "ok", "data": { }, "traceId": "..." }
```

## 二、业务错误码
| 错误码 | HTTP | 含义 |
| --- | --- | --- |
| `AUTH_REQUIRED` | 401 | 未登录 |
| `PERMISSION_DENIED` | 403 | 无权限 |
| `SEAT_ALREADY_RESERVED` | 409 | 座位/时间片已占 |
| `SEAT_ALREADY_HELD` | 409 | 座位正被他人临时选择中 |
| `RESERVATION_TIME_CONFLICT` | 409 | 用户自身时段冲突 |
| `DAILY_LIMIT_EXCEEDED` | 400 | 超单日次数 |
| `USER_IN_BLACKLIST` | 403 | 黑名单 |
| `SIGN_IN_TIMEOUT` | 400 | 签到超时 |
| `SIGN_IN_TOO_EARLY` | 400 | 未到签到时间 |
| `USERNAME_EXISTS` | 400 | 注册用户名已存在 |
| `RESERVATION_NOT_FOUND` | 404 | 预约不存在 |
| `INVALID_TIME_RANGE` | 400 | 时间非法 |
| `SCORE_RULE_NOT_FOUND` | 400 | 积分规则缺失 |
| `NO_AVAILABLE_ROOM_NEARBY` | 404 | 附近无空位 |
| `GEO_LOCATION_REQUIRED` | 400 | 需要位置 |

## 三、接口模块与清单
### 3.1 登录认证
| 方法 | URL | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | 公共 | 登录（密码 BCrypt 校验） |
| POST | `/api/auth/register` | 公共 | 自助注册（默认 STUDENT，注册即登录，需图形验证码） |
| POST | `/api/auth/reset-password` | 公共 | 重置密码（用户名+姓名验证身份，需图形验证码） |
| POST | `/api/auth/logout` | 登录 | 登出 |

> **密码安全**：已实现——注册/迁移均以 **BCrypt** 存储，登录用 `matches` 校验；启动时自动将历史明文密码升级为 BCrypt。传输层仍建议部署 HTTPS。

> **注册功能**当前不纳入 MVP，用户由管理员预置 / 种子数据初始化。注册接口 `POST /api/auth/register` 为后续扩展预留。详见 [docs/05-extension-design.md](../docs/05-extension-design.md)。

> **密码安全**：当前登录接口密码以**明文传输**，存在安全隐患。生产环境必须启用 HTTPS；后端存储时需对密码加密（具体加密算法待确认）。详见 [docs/08-known-issues.md](../docs/08-known-issues.md) §P4。
响应：
```json
{ "code":0,"message":"ok","data":{ "token":"...","role":"STUDENT","userInfo":{"id":1,"realName":"张三","creditScore":10} } }
```

### 3.2 用户信息
| 方法 | URL | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/users/me` | 登录 | 当前用户 |

### 3.3 校区/楼栋/自习室/座位
| 方法 | URL | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/campuses` | 登录 | 校区列表 |
| POST/PUT/DELETE | `/api/campuses/**` | ADMIN | 校区维护 |
| GET | `/api/buildings?campusId=` | 登录 | 楼栋列表 |
| POST/PUT/DELETE | `/api/buildings/**` | ADMIN | 楼栋维护 |
| GET | `/api/study-rooms?campusId=&buildingId=&floorNo=` | 登录 | 自习室筛选 |
| POST/PUT/DELETE | `/api/study-rooms/**` | ADMIN | 自习室维护 |
| GET | `/api/study-rooms/{id}/layout` | 登录 | 座位排布 |
| PUT | `/api/study-rooms/{id}/layout` | ADMIN | 保存排布 |
| POST | `/api/study-rooms/{id}/generate-layout?rows=&cols=&aisleCol=` | ADMIN | 按行列快速生成座位网格 |
| POST | `/api/seats/{seatId}/toggle?enabled=` | ADMIN | 座位启用/禁用 |

`GET /api/study-rooms/{id}/layout` 响应：
```json
{ "code":0,"data":{ "rows":6,"cols":8,"cells":[{"rowIndex":0,"colIndex":0,"cellType":"SEAT","seatId":101,"seatNo":"A-01","enabled":true}] } }
```

### 3.4 座位看板（快照）
| 方法 | URL | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/study-rooms/{id}/board?date=&start=&end=` | 登录 | 看板快照 |

响应（board_snapshot）：
```json
{ "code":0,"data":{ "roomId":10,"date":"2026-07-06","seats":[{"seatId":101,"seatNo":"A-01","status":"FREE","mine":false}] } }
```

### 3.5-0 临时锁座（分布式实时特色，详见 server/15）
| 方法 | URL | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/holds` | STUDENT | 点座临时保留（Redis TTL 90s），返回 `{expireAt,holdSeconds}` |
| POST | `/api/holds/release` | STUDENT | 释放本人锁 |

### 3.5 预约 / 签到 / 签退 / 取消
| 方法 | URL | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/reservations` | STUDENT | 提交预约 |
| GET | `/api/reservations/me` | STUDENT | 我的预约 |
| POST | `/api/reservations/{id}/check-in` | STUDENT | 签到（PENDING_SIGN_IN→IN_USE） |
| POST | `/api/reservations/{id}/check-out` | STUDENT | 签退（IN_USE→COMPLETED） |
| POST | `/api/reservations/{id}/cancel` | STUDENT | 取消 |
| GET | `/api/admin/reservations?keyword=&status=&date=` | ADMIN | 按学生姓名/用户名追踪预约 |

> 预约响应新增字段：`signinStart`/`signinDeadline`（签到窗口）、`scoreDelta`（签退/取消的积分变化）。
> 创建约束：`预约开始时间必须晚于当前时间`；签到须在窗口内，早于窗口返回 `SIGN_IN_TOO_EARLY`。

请求 `POST /api/reservations`：
```json
{ "roomId":10,"seatId":101,"date":"2026-07-06","startTime":"14:00","endTime":"16:00" }
```
成功响应：
```json
{ "code":0,"message":"ok","data":{ "reservationId":5001,"status":"PENDING_SIGN_IN" } }
```
失败响应（并发）：
```json
{ "code":"SEAT_ALREADY_RESERVED","message":"座位已被抢占","data":null,"traceId":"..." }
```
业务错误码：`SEAT_ALREADY_RESERVED`,`DAILY_LIMIT_EXCEEDED`,`USER_IN_BLACKLIST`,`INVALID_TIME_RANGE`,`RESERVATION_TIME_CONFLICT`。

`check-in` 错误码：`SIGN_IN_TIMEOUT`,`RESERVATION_NOT_FOUND`,`PERMISSION_DENIED`。

`check-out` 请求 `POST /api/reservations/{id}/check-out`（无请求体）：仅当状态为 `IN_USE` 且属主匹配时可签退。
成功响应：
```json
{ "code":0,"message":"ok","data":{ "reservationId":5001,"status":"COMPLETED" } }
```
签退后：座位由 USING 释放为 FREE、删除对应 `reservation_slot` 行、推送 SSE `seat_released`，并（MVP+）结算积分 +2。
`check-out` 错误码：`RESERVATION_NOT_FOUND`,`PERMISSION_DENIED`（状态非 IN_USE 视为非法操作）。

> 注：若学生未主动签退，由**自动完成任务**在预约结束时间到达后将 `IN_USE→COMPLETED` 并释放座位（见 [06-timeout-release-and-blacklist](06-timeout-release-and-blacklist.md) §八）。

### 3.6 黑名单
| 方法 | URL | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/blacklist/me` | STUDENT | 本人黑名单状态 |
| GET | `/api/admin/blacklist` | ADMIN | 黑名单列表 |
| POST | `/api/admin/blacklist/{id}/release` | ADMIN | 解除 |

### 3.7 实时看板（SSE）
| 方法 | URL | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/board/stream?roomId=&date=&start=&end=&token=` | 登录 | SSE 订阅 |

> **SSE 鉴权**：浏览器原生 `EventSource` 不能自定义请求头，无法携带 `satoken` 头，因此 SSE 端点的 token 通过 **查询参数 `token=<token>`** 传递（服务端据此校验登录态）；其余 REST 接口仍走请求头。详见 [07-sse-realtime-board](07-sse-realtime-board.md) §鉴权 与 [04-auth-rbac](04-auth-rbac.md)。

事件类型与示例见 [07-sse-realtime-board](07-sse-realtime-board.md)。

### 3.8 统计报表
| 方法 | URL | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/reports/usage?...` | ADMIN | 使用率 |
| GET | `/api/reports/peak-slots?...` | ADMIN | 热门时段 |
| GET | `/api/reports/cancel-rate?...` | ADMIN | 取消率 |
| GET | `/api/reports/no-show-rate?...` | ADMIN | 爽约率 |
| GET | `/api/reports/room-ranking?...` | ADMIN | 利用率排行 |

筛选参数：`campusId,buildingId,roomId,startDate,endDate`。

### 3.9 积分排名（MVP+）
| 方法 | URL | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/scores/me` | STUDENT | 本人积分与流水 |
| GET | `/api/scores/ranking?period=week|month` | 登录 | 排行榜 |
| GET | `/api/admin/scores?userId=` | ADMIN | 积分记录 |

### 3.10 最近空位推荐（MVP+）
| 方法 | URL | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/rooms/availability-summary?campusId=` | 登录 | 空位概览 |
| GET | `/api/rooms/nearest-available?originType=&originId=&date=&start=&end=` | STUDENT | 附近空位推荐 |

`nearest-available` 响应：
```json
{ "code":0,"data":[{"roomId":10,"roomName":"A301","distance":120.5,"availableSeats":8,"open":true}] }
```
错误码：`NO_AVAILABLE_ROOM_NEARBY`,`GEO_LOCATION_REQUIRED`。

## 实现约束
- 所有写接口经 service 事务；预约走锁+唯一索引。
- 错误码取值与 GLOSSARY 一致；新增码同步两处。

## 验收标准
- 每接口有权限校验；示例与实现一致；错误码可复现。

## 给 AI Coding Agent 的提示
改接口先更新本文件与前端 [../client/07-api-calling-design.md](../client/07-api-calling-design.md)；保持请求/响应结构稳定，避免破坏前端 mock。
