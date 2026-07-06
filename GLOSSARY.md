# GLOSSARY.md · 统一术语表

- **文档目的**：统一全项目中文名 / 英文名 / 代码命名 / 数据库命名，避免命名漂移。
- **适用范围**：全项目文档与代码。
- **读者对象**：全体开发者与 Coding Agent。
- **相关文件**：[server/01-domain-model.md](server/01-domain-model.md)、[server/02-database-schema.md](server/02-database-schema.md)、[server/03-api-design.md](server/03-api-design.md)。

## 关键结论
命名一次确定，全项目复用。**任何新代码不得引入与本表冲突的别名**（如用 `Room`/`Booking` 代替 `StudyRoom`/`Reservation`）。

## 一、核心业务术语
| 中文名 | 英文名 | 代码命名(类/变量) | 数据库命名(表/字段) | 说明 | 常见误解 |
| --- | --- | --- | --- | --- | --- |
| 校区 | Campus | `Campus` | `campus` | 学校校区，含坐标字段 | 不等于楼栋 |
| 楼栋 | Building | `Building` | `building` | 校区下的建筑 | 不含楼层信息本身 |
| 楼层 | Floor | `Floor`/`floorNo` | `study_room.floor_no` | 用楼层号表达，不单独建表(MVP) | 非独立实体 |
| 自习室 | StudyRoom | `StudyRoom` | `study_room` | 预约的物理空间 | 别写成 `Room`/`ClassRoom` |
| 座位 | Seat | `Seat` | `seat` | 网格中 SEAT 类型单元 | 只有 SEAT 可预约 |
| 座位单元格 | SeatCell | `SeatCell` | 存于 `seat`/布局JSON | 网格单元，有 cell_type | AISLE/EMPTY 不可约 |
| 预约(主记录) | Reservation | `Reservation` | `reservation` | 一次预约的主记录 | 不直接存时间重叠判断 |
| 时间片占用 | ReservationSlot | `ReservationSlot` | `reservation_slot` | 座位×日期×时间片的占用行 | 并发兜底在这里 |
| 时间片 | Slot / slotIndex | `slotIndex` | `reservation_slot.slot_index` | 30 分钟粒度序号 | 不是分钟数 |
| 黑名单记录 | BlacklistRecord | `BlacklistRecord` | `blacklist_record` | 爽约超阈值产生 | 限制预约不限制登录 |
| 积分 | Score | `Score`/`scoreValue` | `sys_user.credit_score` | 激励分，非权限 | 非强约束 |
| 积分流水 | ScoreRecord | `ScoreRecord` | `score_record` | 每次加减分记录 | 必须留痕 |
| 爽约 | NoShow | `noShowCount` | `sys_user.no_show_count` | 超时未签到 | 主动取消一般不算 |
| 签到 | CheckIn | `checkIn` | `reservation.check_in_time` | 到场签到 | 超时窗口默认 15 分钟 |
| 签退 | CheckOut | `checkOut` | `reservation.check_out_time` | 正常结束 | 未签退可选扣分 |
| 超时释放 | TimeoutRelease | `TimeoutReleaseJob` | — | 超时自动释放座位 | 延迟队列为主，全表扫描兜底 |
| 座位热力图 | Heatmap / Board | `HeatmapBoard` | — | 座位状态可视化 | 快照+SSE，非轮询 |
| 附近空位推荐 | NearestAvailableRoom | `NearestAvailableRoomService` | — | 就近有空位推荐 | MVP 手动选位置 |
| 房间位置 | RoomLocation | `RoomLocation` | `room_location_snapshot`(独立表时) | 位置坐标；MVP 直接并入 campus/building/study_room 的坐标字段 | 独立表 `room_location_snapshot` 仅为可选扩展，非默认 |

## 二、状态枚举
| 类别 | 枚举值 | 含义 |
| --- | --- | --- |
| 预约状态 ReservationStatus | `PENDING_SIGN_IN` | 预约成功待签到 |
| | `IN_USE` | 已签到使用中 |
| | `COMPLETED` | 已签退完成 |
| | `CANCELLED` | 主动取消 |
| | `EXPIRED_RELEASED` | 超时未签到已释放 |
| 座位状态 SeatStatus | `FREE` | 空闲 |
| | `RESERVED` | 已预约(待签到) |
| | `USING` | 使用中 |
| | `DISABLED` | 不可用/禁用 |
| 单元格类型 CellType | `SEAT` | 可预约座位 |
| | `AISLE` | 过道 |
| | `EMPTY` | 空白占位 |
| | `DISABLED` | 禁用座位 |
| 角色 Role | `STUDENT` / `ADMIN` | 学生 / 管理员 |

## 三、业务错误码
| 错误码 | 含义 |
| --- | --- |
| `AUTH_REQUIRED` | 未登录 |
| `PERMISSION_DENIED` | 无权限 |
| `SEAT_ALREADY_RESERVED` | 座位/时间片已被占用 |
| `RESERVATION_TIME_CONFLICT` | 用户自身时间冲突 |
| `DAILY_LIMIT_EXCEEDED` | 超单日预约次数 |
| `USER_IN_BLACKLIST` | 黑名单用户 |
| `SIGN_IN_TIMEOUT` | 签到超时 |
| `RESERVATION_NOT_FOUND` | 预约不存在 |
| `INVALID_TIME_RANGE` | 时间范围非法 |
| `SCORE_RULE_NOT_FOUND` | 积分规则缺失 |
| `NO_AVAILABLE_ROOM_NEARBY` | 附近无空位 |
| `GEO_LOCATION_REQUIRED` | 需要位置信息 |

## 四、命名规范
- 数据库：`snake_case`，表名单数（`study_room`），布尔用 `is_xxx`，时间用 `xxx_time`。
- Java：类 `PascalCase`，方法/字段 `camelCase`，常量 `UPPER_SNAKE`。
- 接口：`/api/{resource}`，资源用复数（`/api/reservations`），动作用子路径（`/api/reservations/{id}/check-in`）。
- 枚举值：`UPPER_SNAKE`，全项目一致。

## 给 AI Coding Agent 的提示
生成代码/接口/建表前先查本表；若需要新术语，先在此登记再使用。禁止同义词混用。
