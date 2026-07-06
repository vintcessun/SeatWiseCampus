# server/10 · 最近空位推荐设计（MVP+）

- **文档目的**：定义“自动定位最近有空位自习室”的方案、位置字段、距离与排序、接口。
- **适用范围**：附近空位推荐（P8）。
- **读者对象**：后端/Agent。
- **相关文件**：[02-database-schema](02-database-schema.md)、[03-api-design](03-api-design.md)、[../docs/05-extension-design.md](../docs/05-extension-design.md)、[../client/01-page-route-map.md](../client/01-page-route-map.md)。

## 关键结论
- MVP 不接真实 GPS：**管理员配坐标 + 学生手动选当前位置**即可推荐。
- 推荐结果**绝不包含无空位自习室**；无结果返回 `NO_AVAILABLE_ROOM_NEARBY`。

## 一、业务目标
学生打开系统即可推荐“距离最近且有空位”的自习室，适配多校区/多楼栋，可列表或（后续）地图展示。

## 二、MVP 简化方案
1. 管理员为 campus/building/study_room 配置经纬度或平面坐标（`latitude/longitude/map_x/map_y`）。
2. 学生手动选择当前校区/楼栋作为当前位置（`originType=building|campus, originId`）。
3. 系统按 **距离 + 空位数 + 开放状态** 推荐。

## 三、增强方案
- 浏览器 Geolocation 获取位置；失败回退手动选择（返回 `GEO_LOCATION_REQUIRED` 提示前端引导）。
- 不强依赖室内定位。
- 排序进一步纳入：未来 30 分钟可约情况、历史热门度、用户偏好。

## 四、位置字段设计
| 表 | 字段 |
| --- | --- |
| campus | latitude,longitude,map_x,map_y |
| building | latitude,longitude,map_x,map_y |
| study_room | latitude,longitude,map_x,map_y |
坐标随建表预留（见 [02](02-database-schema.md)），MVP 可先用平面坐标近似。

## 五、距离计算
| 方案 | 说明 |
| --- | --- |
| 平面坐标(MVP) | 欧氏距离 `sqrt((x1-x2)^2+(y1-y2)^2)`，同校区足够 |
| 经纬度(增强) | Haversine 计算球面距离 |
优先同楼栋（距离视为 0 或最小），再跨楼栋按坐标。

## 六、空位查询
指定 `date + 时段`，某自习室可用座位数 = `enabled 且 cell_type=SEAT 的座位` 中，对应 `slot_index` 无 `reservation_slot` 占用的数量。可用 Redis 缓存概览加速（`availability-summary`）。

## 七、推荐排序公式
完整公式（增强）：
```
score = distance_weight * normalized_distance
      + availability_weight * available_seat_ratio   (取负向或反距离，使空位越多越靠前)
      + popularity_weight  * inverse_popularity
```
**MVP 简化排序**（优先用此）：
```
优先同楼栋 > 同校区距离最近 > 空位数更多
```
即先按“是否同楼栋”，再按距离升序，再按空位数降序。

## 八、接口设计
| 接口 | 说明 |
| --- | --- |
| `GET /api/rooms/availability-summary?campusId=` | 各房间空位概览 |
| `GET /api/rooms/nearest-available?originType=&originId=&date=&start=&end=` | 附近空位推荐 |

响应：
```json
{ "code":0,"data":[{"roomId":10,"roomName":"A301","buildingName":"图书馆A座","distance":0,"availableSeats":8,"open":true}] }
```

## 九、数据库索引
- `study_room(building_id, status)`、`seat(room_id, cell_type, enabled)` 辅助空位统计。
- `reservation_slot` 唯一索引已支持按 `date+slot_index` 过滤占用。

## 十、前端展示建议
`/student/nearby` 列表卡片：房间名、楼栋、距离、剩余空位、是否开放、一键进入选座。后续可挂校园地图组件。

## 十一、定位失败与无空位处理
| 情况 | 处理 |
| --- | --- |
| 定位失败 | 返回/提示 `GEO_LOCATION_REQUIRED`，前端回退手动选位置 |
| 附近无空位 | 返回 `NO_AVAILABLE_ROOM_NEARBY`，前端空态提示 |

## 十二、后续校园地图扩展
用平面图 + `map_x/map_y` 打点；空位数用色彩/标签表达；点位跳转选座。属后续扩展（见 [../docs/05](../docs/05-extension-design.md) E）。

## 实现约束
- 推荐结果过滤掉无空位与未开放房间。
- MVP 用简化排序，不引入复杂权重调参。

## 验收标准
- 不返回无空位自习室；排序符合“同楼栋 > 同校区最近 > 空位更多”；定位失败可回退。

## 给 AI Coding Agent 的提示
MVP 先做手动选位置 + 简化排序；坐标字段可随 MVP 建表预留，但推荐逻辑属 P8，勿提前接浏览器定位与地图。
