# docs/03 · 核心业务流程

- **文档目的**：用流程图/时序图刻画关键业务路径，作为前后端实现的共同参照。
- **适用范围**：预约主线全流程。
- **读者对象**：前后端/测试/Agent。
- **相关文件**：[server/05](../server/05-reservation-concurrency-control.md)、[server/06](../server/06-timeout-release-and-blacklist.md)、[server/07](../server/07-sse-realtime-board.md)、[client/02](../client/02-student-side-design.md)。

## 关键结论
- 所有“写”类流程都以后端事务为准；前端只驱动与展示。
- 预约成功后一切状态流转（签到/超时/取消）都会触发 SSE 与（可选）积分变化。

## 一、学生登录
```mermaid
sequenceDiagram
    participant C as client
    participant S as server(Sa-Token)
    C->>S: POST /api/auth/login {username,password}
    S->>S: 校验凭据、生成 token、加载角色
    S-->>C: {token, role, userInfo}
    C->>C: 保存 token 到 userStore，路由跳转
```

## 二、筛选自习室
```mermaid
flowchart LR
    A[选校区] --> B[选楼栋] --> C[选楼层] --> D[选自习室]
    D --> E[GET /api/study-rooms?campusId&buildingId&floorNo]
    E --> F[展示房间卡片 + 开放状态 + 空位概览]
```

## 三、查询座位状态
```mermaid
sequenceDiagram
    participant C as client
    participant S as server
    C->>S: GET /api/study-rooms/{id}/board?date&start&end
    S->>S: 读座位排布 + Redis 状态缓存(缺失回源 MySQL)
    S-->>C: board_snapshot(座位网格 + 每格状态)
    C->>S: 建立 SSE 订阅(同参数)
    S-->>C: 后续增量事件
```

## 四、提交预约（并发控制，核心）
```mermaid
sequenceDiagram
    participant C as client
    participant S as server
    participant R as Redisson
    participant DB as MySQL
    C->>S: POST /api/reservations {roomId,seatId,date,startTime,endTime}
    S->>S: 校验登录/角色/黑名单/时间/单日次数/自身时段冲突
    S->>S: 起止时间→slotIndex 列表
    S->>R: lock seat:{seatId}:date:{date}:slots:{range}
    S->>DB: BEGIN; insert reservation; batch insert reservation_slot
    alt 唯一索引冲突
        DB-->>S: DuplicateKey
        S->>S: 回滚
        S-->>C: SEAT_ALREADY_RESERVED
    else 成功
        S->>DB: COMMIT
        S->>S: 写 Redis 座位状态=RESERVED
        S->>S: 注册超时释放延迟任务
        S->>S: 推送 SSE seat_reserved
        S->>R: unlock
        S-->>C: 预约成功(reservationId)
    end
```
详细步骤见 [server/05](../server/05-reservation-concurrency-control.md)。

## 五、签到
```mermaid
sequenceDiagram
    participant C as client
    participant S as server
    C->>S: POST /api/reservations/{id}/check-in
    S->>S: 校验本人、状态=PENDING_SIGN_IN、在签到窗口内
    alt 超过签到窗口
        S-->>C: SIGN_IN_TIMEOUT
    else 成功
        S->>S: 状态→IN_USE, 记录 check_in_time, 取消超时释放任务
        S->>S: 投递自动完成任务(到期=预约结束时间)
        S->>S: Redis 座位=USING, 推送 SSE seat_in_use
        S-->>C: 签到成功
    end
```

## 五之二、签退 / 自动完成
```mermaid
flowchart TB
    A[IN_USE 使用中] --> B{结束时间前主动签退?}
    B -- 是 --> C[POST /reservations/id/check-out]
    B -- 否 --> D[自动完成任务到期]
    C --> E[COMPLETED, 记录 check_out_time, +2 MVP+]
    D --> F[COMPLETED, 系统记录 check_out_time]
    E --> G[释放 slot, Redis=FREE, SSE seat_released]
    F --> G
```
详见 [server/06 §八](../server/06-timeout-release-and-blacklist.md)。

## 六、取消预约
```mermaid
flowchart TB
    A[POST /api/reservations/id/cancel] --> B{状态可取消?}
    B -- 否 --> E[RESERVATION_NOT_FOUND / 非法状态]
    B -- 是 --> C{距开始>30分钟?}
    C -- 是 --> D[CANCELLED 不扣分]
    C -- 否 --> F[CANCELLED 扣1分(MVP+)]
    D --> G[释放 slot, Redis=FREE, SSE seat_released]
    F --> G
```

## 七、超时释放
```mermaid
sequenceDiagram
    participant J as Redisson DelayedQueue
    participant S as server
    participant DB as MySQL
    J-->>S: 到期任务(reservationId)
    S->>DB: 校验仍为 PENDING_SIGN_IN
    alt 已签到/已取消
        S->>S: 忽略
    else 仍待签到
        S->>DB: 状态→EXPIRED_RELEASED, 释放 slot
        S->>S: no_show_count+1, 扣3分(MVP+)
        S->>S: Redis=FREE, 推送 SSE seat_released
        S->>S: 达阈值→写 blacklist_record
    end
```

## 八、黑名单
```mermaid
flowchart LR
    A[爽约计数达阈值] --> B[创建 blacklist_record 有效期7天]
    B --> C[预约接口校验命中→USER_IN_BLACKLIST]
    B --> D[到期自动失效/解除]
    C -. 仍可登录/查看历史 .-> E[不限制读]
```

## 九、管理员维护座位
```mermaid
flowchart LR
    A[进入排布编辑器] --> B[编辑行列网格/cell_type/seat_no]
    B --> C[PUT /api/study-rooms/id/layout {layoutJson}]
    C --> D[后端校验并落库 seat 表]
    D --> E[看板/查询读取新排布]
```

## 十、实时看板更新
```mermaid
sequenceDiagram
    participant C1 as client A
    participant C2 as client B
    participant S as server
    C1->>S: 预约座位成功
    S->>S: 生成 seat_reserved 事件
    S-->>C1: SSE seat_reserved
    S-->>C2: SSE seat_reserved
    C2->>C2: 局部更新该座位为 RESERVED
```

## 十一、积分变化（MVP+）
```mermaid
flowchart LR
    A[正常签退] --> P1[+2]
    B[临近30min取消] --> P2[-1]
    C[超时未签到] --> P3[-3]
    P1 & P2 & P3 --> R[写 score_record + 更新 credit_score]
```

## 十二、最近空位推荐（MVP+）
```mermaid
flowchart LR
    A[选/定位当前位置] --> B[GET /api/rooms/nearest-available]
    B --> C[按距离+空位+开放状态排序]
    C --> D{有空位?}
    D -- 是 --> E[返回推荐列表]
    D -- 否 --> F[NO_AVAILABLE_ROOM_NEARBY]
```

## 实现约束
- 图中所有“状态变更”均在后端事务内完成并触发 SSE。
- 超时释放以延迟任务为主、全表扫描为兜底。

## 验收标准
- 每个流程可在 [docs/07](07-acceptance-checklist.md) 找到对应验收项。

## 给 AI Coding Agent 的提示
实现某流程前对照本图与对应 server/client 文档；不要新增图外的隐式状态转移。
