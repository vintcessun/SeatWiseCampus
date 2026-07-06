# client · 前端文档总览

- **文档目的**：作为前端工程文档入口，给出定位、技术栈、模块与阅读顺序。
- **适用范围**：`client/` 前端工程。
- **读者对象**：前端开发/Agent。
- **相关文件**：[client/AGENTS.md](AGENTS.md)、[client/00-client-overview.md](00-client-overview.md)、[../docs/02-system-architecture.md](../docs/02-system-architecture.md)。

## 关键结论
- 前端只做**展示 + 交互 + 表单校验 + 调用 API/SSE**，**不判定预约最终结果**。
- 座位状态来自“快照 + SSE”，不在前端自行推算占用。

## 一、前端定位
SeatWise Campus 的客户端，服务两类用户：**学生端**（预约主线）与**管理员端**（基础数据 + 看板 + 报表）。通过 REST 与 SSE 同后端交互。

## 二、技术栈
Vue 3 + Vite + Element Plus + ECharts + Axios + Pinia + Vue Router。

## 三、学生端 / 管理端 / 实时看板
| 模块 | 核心页面 |
| --- | --- |
| 学生端 | 登录、筛选、选座、我的预约、签到、黑名单提示、（排行榜、附近空位） |
| 管理端 | 登录、校区/楼栋/自习室管理、排布编辑、实时看板、报表、黑名单、（积分记录） |
| 实时看板 | SeatGrid + HeatmapBoard，快照 + SSE 增量 |

## 四、文档索引
| 文件 | 说明 |
| --- | --- |
| [00-client-overview](00-client-overview.md) | 客户端整体架构 |
| [01-page-route-map](01-page-route-map.md) | 页面与路由表 |
| [02-student-side-design](02-student-side-design.md) | 学生端设计 |
| [03-admin-side-design](03-admin-side-design.md) | 管理员端设计 |
| [04-seat-grid-and-heatmap](04-seat-grid-and-heatmap.md) | 座位网格与热力图 |
| [05-component-design](05-component-design.md) | 组件设计 |
| [06-state-management](06-state-management.md) | Pinia 状态管理 |
| [07-api-calling-design](07-api-calling-design.md) | API 调用设计 |
| [08-frontend-validation-and-error-handling](08-frontend-validation-and-error-handling.md) | 校验与错误处理 |
| [09-client-test-and-acceptance](09-client-test-and-acceptance.md) | 前端测试验收 |
| [10-llm-client-implementation-guide](10-llm-client-implementation-guide.md) | 前端实现指南(LLM) |

## 五、推荐阅读顺序
00 架构 → 01 路由 → 07 API → 06 状态 → 04 看板 → 05 组件 → 02/03 端设计 → 08 校验 → 10 实现指南。

## 六、MVP 范围
登录、筛选、选座预约、我的预约、签到、黑名单提示、管理端基础数据/排布/看板/报表。

## 七、扩展范围
排行榜页、附近空位页、积分展示、通知中心、校园地图（后续）。

## 给 AI Coding Agent 的提示
改前端先读 [AGENTS.md](AGENTS.md)；新增页面/组件/接口/store 必须回写对应文档。
