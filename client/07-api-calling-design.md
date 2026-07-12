# client/07 · API 调用设计

- **文档目的**：定义 Axios 实例、统一响应/错误处理、token 注入、SSE 封装与 API 模块划分。
- **适用范围**：`client/src/api`、`client/src/sse`。
- **读者对象**：前端/Agent。
- **相关文件**：[../server/03-api-design.md](../server/03-api-design.md)、[08-frontend-validation-and-error-handling](08-frontend-validation-and-error-handling.md)、[06-state-management](06-state-management.md)。

## 关键结论
- 所有请求走统一 Axios 实例；token 注入与错误码处理集中在拦截器。
- SSE 单独封装，支持心跳与重连，事件分发到 heatmapStore。

## 一、Axios 实例
```
baseURL: import.meta.env.VITE_API_BASE_URL  // 如 /api
timeout: 10000
```
组件/Store 不硬编码 baseURL。

## 二、Token 注入
请求拦截器从 userStore 取 token，注入 Sa-Token 约定的请求头（如 `satoken: <token>`，具体以 [../server/04-auth-rbac.md](../server/04-auth-rbac.md) 为准）。

## 三、统一响应结构
```json
{ "code": 0, "message": "ok", "data": {}, "traceId": "..." }
```
响应拦截器：`code===0` 返回 `data`；否则抛业务错误（携带 code/message）。

## 四、统一错误处理
| 情况 | 处理 |
| --- | --- |
| HTTP 401 / `AUTH_REQUIRED` | 清 token，跳 `/login` |
| HTTP 403 / `PERMISSION_DENIED` | 提示无权限 |
| 业务错误码(code≠0) | 映射文案 toast（见错误码表） |
| 网络/超时 | 提示网络异常，可重试 |

错误码文案映射集中在 `utils/errorCode.ts`，键取自 [GLOSSARY.md](../GLOSSARY.md)。

## 五、401/403 处理
401 统一登出跳登录；403 停留当前页并提示；两者不吞错，记录 traceId 便于排查。

## 六、业务错误码展示
`SEAT_ALREADY_RESERVED`→“座位已被抢占，请重选”并刷新座位；`DAILY_LIMIT_EXCEEDED`→“今日预约已达上限”；`USER_IN_BLACKLIST`→跳黑名单提示页；等等。

## 七、SSE 封装
```
sse/boardStream.ts
- connect({roomId,date,start,end}, handlers)
- 心跳超时检测 -> 触发重连
- 指数退避重连 -> 重连后回调 onReconnect(重取快照)
- disconnect()
```
事件 `board_snapshot/seat_reserved/seat_released/seat_in_use/seat_disabled/heartbeat` 分发到 heatmapStore。详见 [04](04-seat-grid-and-heatmap.md)。

**SSE 鉴权**：浏览器原生 `EventSource` 不能自定义请求头，无法带 `satoken` 头。因此建连 URL 需拼接 **查询参数 `token=<token>`**（token 取自 userStore）：`/api/board/stream?roomId=&date=&start=&end=&token=${token}`。其余 REST 请求仍走请求头。与后端约定见 [../server/07-sse-realtime-board.md](../server/07-sse-realtime-board.md)。

## 八、API 模块划分
| 模块 | 文件 | 覆盖 |
| --- | --- | --- |
| auth | `api/index.js` | 登录/注册/登出/me/验证码/重置密码 |
| room | `api/room.ts` | 校区/楼栋/自习室/座位/board |
| reservation | `api/reservation.ts` | 预约/签到/取消/我的 |
| admin | `api/admin.ts` | 基础数据 CRUD/排布/黑名单 |
| report | `api/report.ts` | 报表 |
| score | `api/score.ts` | 积分/排行(MVP+) |
| nearby | `api/nearby.ts` | 附近空位(MVP+) |

## 九、前端 mock 策略
- 后端未就绪时用可切换 mock（`VITE_USE_MOCK`），mock 数据结构必须与真实响应一致。
- SSE 可用本地定时器模拟事件，联调后移除。

## 实现约束
- 所有 API 经模块函数，不在组件内直接 `axios(...)`。
- mock 与真实响应结构一致，切换零改动业务代码。

## 验收标准
- 抓包仅见 `/api/**` 与 SSE；错误码有统一文案；SSE 断线可重连。

## 给 AI Coding Agent 的提示
新增接口先在对应 api 模块加函数并在本表登记；错误码文案统一维护，勿在组件内散写。
