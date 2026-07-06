# server/02 · MySQL 8 数据库设计

- **文档目的**：定义表结构、索引与并发/扩展相关的关键约束。
- **适用范围**：MySQL 8 主存储。
- **读者对象**：后端/DBA/Agent。
- **相关文件**：[01-domain-model](01-domain-model.md)、[05-reservation-concurrency-control](05-reservation-concurrency-control.md)、[10-nearest-available-room-design](10-nearest-available-room-design.md)。

## 关键结论
- **`reservation_slot` 的 `(seat_id, date, slot_index)` 唯一索引是防重复预约的最终兜底，不可删除。**
- 时间片粒度 30 分钟；预约拆片存储便于并发控制与查询。
- 坐标字段预留在 campus/building/study_room，服务附近空位推荐。

## 通用约定
- 引擎 InnoDB，字符集 utf8mb4。
- 所有业务表含 `id BIGINT PK AUTO_INCREMENT`、`created_time DATETIME`、`updated_time DATETIME`。
- 需逻辑删除的表含 `deleted TINYINT DEFAULT 0`（MyBatis-Plus 逻辑删除）。

## 一、sys_user
| 字段 | 类型 | 空 | 默认 | 索引 | 说明 |
| --- | --- | --- | --- | --- | --- |
| id | BIGINT | 否 | 自增 | PK | |
| username | VARCHAR(64) | 否 | | UNIQUE | 登录名 |
| password | VARCHAR(128) | 否 | | | 加密存储 |
| real_name | VARCHAR(64) | 是 | | | 姓名 |
| role | VARCHAR(16) | 否 | STUDENT | IDX | STUDENT/ADMIN |
| credit_score | INT | 否 | 0 | | 积分(MVP+) |
| no_show_count | INT | 否 | 0 | | 爽约累计 |
| deleted | TINYINT | 否 | 0 | | 逻辑删除 |
| created_time/updated_time | DATETIME | | | | |

## 二、campus
| 字段 | 类型 | 空 | 索引 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | 否 | PK | |
| name | VARCHAR(64) | 否 | | 校区名 |
| latitude | DECIMAL(10,7) | 是 | | 纬度(预留) |
| longitude | DECIMAL(10,7) | 是 | | 经度(预留) |
| map_x | INT | 是 | | 平面坐标(预留) |
| map_y | INT | 是 | | 平面坐标(预留) |
| deleted/created_time/updated_time | | | | |

## 三、building
| 字段 | 类型 | 空 | 索引 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | 否 | PK | |
| campus_id | BIGINT | 否 | IDX | 所属校区 |
| name | VARCHAR(64) | 否 | | 楼栋名 |
| latitude/longitude/map_x/map_y | | 是 | | 位置(预留) |
| deleted/created_time/updated_time | | | | |

## 四、study_room
| 字段 | 类型 | 空 | 默认 | 索引 | 说明 |
| --- | --- | --- | --- | --- | --- |
| id | BIGINT | 否 | | PK | |
| building_id | BIGINT | 否 | | IDX | 所属楼栋 |
| floor_no | INT | 否 | | IDX(building_id,floor_no) | 楼层号 |
| name | VARCHAR(64) | 否 | | | 房间名 |
| open_start | TIME | 否 | 08:00 | | 开放开始 |
| open_end | TIME | 否 | 22:00 | | 开放结束 |
| status | VARCHAR(16) | 否 | OPEN | | OPEN/CLOSED |
| latitude/longitude/map_x/map_y | | 是 | | | 位置(预留) |
| deleted/created_time/updated_time | | | | | |

## 五、seat
| 字段 | 类型 | 空 | 默认 | 索引 | 说明 |
| --- | --- | --- | --- | --- | --- |
| id | BIGINT | 否 | | PK | |
| room_id | BIGINT | 否 | | IDX | 所属自习室 |
| row_index | INT | 否 | | | 行 |
| col_index | INT | 否 | | | 列 |
| cell_type | VARCHAR(16) | 否 | SEAT | | SEAT/AISLE/EMPTY/DISABLED |
| seat_no | VARCHAR(16) | 是 | | UNIQUE(room_id,seat_no) | 座位编号(仅SEAT) |
| enabled | TINYINT | 否 | 1 | | 运营态启用/禁用开关 |
| deleted/created_time/updated_time | | | | | |

> **两种“禁用”语义区分**（避免歧义）：
> - `cell_type = DISABLED`：**排布态**，该单元格在座位图上就是一个不可用占位（与 SEAT/AISLE/EMPTY 同层），永久不可预约，不生成 seat_no。
> - `enabled = 0`：**运营态**开关，作用于 `cell_type=SEAT` 的真实座位，用于临时停用（如维修），可随时恢复为 `enabled=1`。
>
> 座位视图状态 `SeatStatus=DISABLED` 的推导优先级：`cell_type=DISABLED` 或 `enabled=0` 任一成立 → DISABLED（不可预约）；否则再按时间片占用推导 FREE/RESERVED/USING。FR-1.7 的“座位启用/禁用”对应操作 `enabled` 字段。

## 六、reservation（主记录）
| 字段 | 类型 | 空 | 索引 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | 否 | PK | |
| user_id | BIGINT | 否 | IDX(user_id,date) | 预约人 |
| seat_id | BIGINT | 否 | IDX | 座位 |
| room_id | BIGINT | 否 | IDX(room_id,date) | 自习室 |
| date | DATE | 否 | | 预约日期 |
| start_slot | INT | 否 | | 起始片序号 |
| end_slot | INT | 否 | | 结束片序号(不含) |
| status | VARCHAR(24) | 否 | IDX | 预约状态 |
| check_in_time | DATETIME | 是 | | 签到时间 |
| check_out_time | DATETIME | 是 | | 签退时间 |
| created_time/updated_time | | | | |

## 七、reservation_slot（时间片占用，并发兜底）
| 字段 | 类型 | 空 | 索引 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | 否 | PK | |
| reservation_id | BIGINT | 否 | IDX | 关联主记录 |
| seat_id | BIGINT | 否 | **UNIQUE(seat_id,date,slot_index)** | 占用唯一键 |
| date | DATE | 否 | ↑ | |
| slot_index | INT | 否 | ↑ | 30 分钟片序号 |
| created_time | DATETIME | | | |

> **`uk_seat_date_slot(seat_id,date,slot_index)` 是防重复预约的最终兜底。释放时删除对应行（或按 reservation_id 批量删）。**

## 八、blacklist_record
| 字段 | 类型 | 空 | 索引 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | 否 | PK | |
| user_id | BIGINT | 否 | IDX(user_id,active) | 用户 |
| reason | VARCHAR(128) | 是 | | 原因 |
| start_time | DATETIME | 否 | | 生效 |
| end_time | DATETIME | 否 | | 到期 |
| active | TINYINT | 否 | | 是否有效 |
| created_time/updated_time | | | | |

## 九、score_record（积分流水，MVP+）
| 字段 | 类型 | 空 | 索引 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | 否 | PK | |
| user_id | BIGINT | 否 | IDX(user_id) | |
| change | INT | 否 | | 变化(+/-) |
| reason | VARCHAR(32) | 否 | | 规则标识 |
| ref_reservation_id | BIGINT | 是 | | 关联预约 |
| created_time | DATETIME | 否 | IDX | |

## 十、room_daily_stats（日聚合）
| 字段 | 类型 | 空 | 索引 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGINT | 否 | PK | |
| room_id | BIGINT | 否 | UNIQUE(room_id,date) | |
| date | DATE | 否 | ↑ | |
| reservation_count | INT | 否 | | 预约数 |
| usage_rate | DECIMAL(5,2) | 否 | | 使用率 |
| cancel_rate | DECIMAL(5,2) | 否 | | 取消率 |
| no_show_rate | DECIMAL(5,2) | 否 | | 爽约率 |
| peak_slot | INT | 是 | | 热门时段 |
| created_time/updated_time | | | | |

## 十一、operation_log
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | PK |
| user_id | BIGINT | 操作人 |
| action | VARCHAR(64) | 动作 |
| detail | VARCHAR(512) | 详情 |
| created_time | DATETIME | |

## 十二、可选扩展表
- `notification(id,user_id,type,title,content,status,created_time,read_time)`。
- `user_preference(id,user_id,fav_building_id,fav_slot_range,...)`。
- `room_location_snapshot(room_id,latitude,longitude,map_x,map_y,updated_time)`（若不并入 study_room）。

## 关键设计解释
| 问题 | 结论 |
| --- | --- |
| 为什么不只在 reservation 判断时间重叠 | 区间重叠判断需读后比较，并发下存在检查-写入竞态，无法用唯一约束保证；且区间比较难加数据库级唯一约束 |
| 为什么 reservation_slot 更适合并发控制 | 拆片后“同座同片”天然可用唯一索引，插入即判重，冲突由数据库原子拒绝 |
| 为什么 Redis 不能作唯一正确性来源 | Redis 无事务级唯一约束保证、可失效/丢数据；只能加速，不能兜底 |
| 哪些字段逻辑删除 | 基础数据(sys_user/campus/building/study_room/seat)用 `deleted` |
| 哪些需要时间戳 | 所有业务表 `created_time`；可变表加 `updated_time` |

## 实现约束
- 唯一索引 `uk_seat_date_slot` 必须存在；释放=删除 slot 行。
- 坐标字段随建表预留，MVP 可为空。

## 验收标准
- 并发插入同座同片时，仅一条成功，其余抛 DuplicateKey。

## 给 AI Coding Agent 的提示
改表先更新本文件；绝不移除 slot 唯一索引；查询空位以 slot 表为准而非扫 reservation 区间。
