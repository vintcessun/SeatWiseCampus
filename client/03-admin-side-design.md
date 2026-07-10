# client/03 · 管理员端设计

- **文档目的**：定义管理端页面与座位排布编辑的交互与数据格式。
- **适用范围**：管理端全部页面。
- **读者对象**：前端/Agent。
- **相关文件**：[01-page-route-map](01-page-route-map.md)、[04-seat-grid-and-heatmap](04-seat-grid-and-heatmap.md)、[../server/02-database-schema.md](../server/02-database-schema.md)、[../docs/14-known-issues-v3.md](../docs/14-known-issues-v3.md)。

## 关键结论
- 管理端核心是**座位排布编辑**与**看板/报表**；排布以布局 JSON 提交后端落库。
- 基础数据严格层级：校区→楼栋→自习室（楼层用楼层号表达）。

## 一、校区/楼栋/楼层/自习室管理
标准表格 CRUD。楼栋关联校区；自习室关联楼栋且带 `floor_no` 与开放时间。删除做逻辑删除与关联校验提示。

> **已知缺陷**：管理端缺少删除自习室功能，新增自习室时楼栋选择固定（硬编码），缺少新增楼栋/校区的能力（详见 [docs/14-known-issues-v3.md](../docs/14-known-issues-v3.md) §R3）。

## 二、自习室管理与开放时间
自习室字段：名称、楼栋、楼层号、开放起止时间、状态。开放时间用于前端时间片可选范围与后端校验。

> **已知缺陷**：自习室缺少「暂时关闭」（CLOSED/MAINTENANCE）状态。管理员因维修/考试占用等场景无法临时关闭，关闭后应禁止新预约且可联动公告通知已预约学生（详见 [docs/14-known-issues-v3.md](../docs/14-known-issues-v3.md) §R4 §R10）。

## 三、座位排布管理（重点）
`AdminLayoutEditor` 编辑行列网格：
| 概念 | 说明 |
| --- | --- |
| 行列网格 | `rows × cols`，每格一个单元 |
| 单元格类型 cell_type | SEAT / AISLE / EMPTY / DISABLED |
| 座位编号 seat_no | 仅 SEAT 类型生成，规则见下 |
| 座位 id seat_id | 后端落库返回 |

> **已知缺陷**：当前排布编辑器仅支持单个过道（`aisleCol`），实际教室存在多过道、每行不同列数等不规则情况。应支持每个单元格独立设置类型（详见 [docs/14-known-issues-v3.md](../docs/14-known-issues-v3.md) §R5）。

**座位编号生成规则（建议）**：按行字母 + 列序号，如 `A-01`（第1行第1个 SEAT），过道/空白不计号；也可按“行号-列号”。规则前后端一致。

**布局 JSON（示例）**：
```json
{
  "rows": 6,
  "cols": 8,
  "cells": [
    { "rowIndex": 0, "colIndex": 0, "cellType": "SEAT", "seatNo": "A-01" },
    { "rowIndex": 0, "colIndex": 1, "cellType": "AISLE" },
    { "rowIndex": 0, "colIndex": 2, "cellType": "SEAT", "seatNo": "A-02" }
  ]
}
```

**保存到服务端**：`PUT /api/study-rooms/{roomId}/layout`，后端据此 upsert `seat` 表并校验编号唯一。保存冲突（如已有预约的座位被删）需提示。

> **已知缺陷**：保存/修改座位排布时未校验该自习室是否存在未来预约，可能导致已预约学生到馆发现座位被移除/移动。建议仅允许在自习室关闭状态下修改座位（详见 [docs/14-known-issues-v3.md](../docs/14-known-issues-v3.md) §R10）。

```mermaid
flowchart LR
    A[加载现有布局 GET layout] --> B[编辑网格/类型/编号]
    B --> C[本地校验: SEAT 才有编号, 编号唯一]
    C --> D[PUT layout 提交]
    D --> E{后端校验}
    E -- 通过 --> F[落库 seat 表, 刷新]
    E -- 冲突 --> G[提示冲突, 不覆盖]
```

## 四、预约记录查询
按自习室/日期/状态筛选查看预约记录（只读）。

> **已知缺陷**：管理端缺乏按学生姓名/学号筛选预约记录的能力，无法追踪具体学生的预约情况（详见 [docs/09-known-issues-v2.md](../docs/09-known-issues-v2.md) §Q3）。

## 五、黑名单查看/解除
展示黑名单记录与到期时间，支持手动解除（`/api/admin/blacklist`）。

## 六、积分记录查看【可选】
展示 `score_record` 流水，按用户/时间筛选。

## 七、数据报表查看
选择校区/楼栋/自习室与时间范围，用 `StatsChartCard`（ECharts）展示使用率、热门时段、取消率、爽约率、利用率排行。数据来自聚合接口。

## 八、实时看板
进入自习室看板，快照 + SSE 实时展示座位状态，用于现场监控。详见 [04](04-seat-grid-and-heatmap.md)。

> **已知缺陷**：管理端概览首页「刷新」按钮样式/位置不佳而几乎不可见，用户无法直观找到手动刷新入口（详见 [docs/14-known-issues-v3.md](../docs/14-known-issues-v3.md) §R2）。

## 九、已知缺项

当前排布编辑器和看板的已知问题已集中迁移至 [docs/08-known-issues.md](../docs/08-known-issues.md) §P1。此章节不再维护，详细信息以 `docs/08` 为准。

### 黑名单解除确认
- 文档 §五 提及支持手动解除，但需确认前端页面是否已实现解禁按钮
- 对应接口 `POST /api/admin/blacklist/{id}/release` 需确认可用

## 实现约束
- 布局 JSON 结构前后端一致；编号规则固定。
- 删除/禁用已被占用座位需后端校验并前端提示。
- 看板颜色语义与学生端保持一致，不要单独实现一套逻辑。

## 验收标准
- 排布可保存/回显；DISABLED 座位学生端不可选；报表可按维度筛选。
- 看板颜色语义正确；未来日期可查。
- 黑名单可手动解除。

## 给 AI Coding Agent 的提示
排布编辑器与选座页共用 SeatCell 渲染；编辑态与预约态是两种交互模式，勿混用。
