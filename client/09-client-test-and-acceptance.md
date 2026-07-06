# client/09 · 前端测试与验收

- **文档目的**：定义前端验收场景与测试要点。
- **适用范围**：`client` 全部功能。
- **读者对象**：前端/测试/Agent。
- **相关文件**：[../docs/07-acceptance-checklist.md](../docs/07-acceptance-checklist.md)、[02-student-side-design](02-student-side-design.md)、[04-seat-grid-and-heatmap](04-seat-grid-and-heatmap.md)。

## 关键结论
- 前端验收以“预约闭环 + 看板实时”为核心；异常路径必须覆盖。

## 一、学生预约流程
- [ ] 登录→筛选→选片→选座→提交→我的预约可见。
- [ ] 提交中按钮禁用防重复。
- [ ] 成功后看板显示本人预约高亮。
- [ ] 签到后可见签退按钮；签退成功后座位在看板释放为 FREE。

## 二、管理员录入座位
- [ ] 排布编辑保存后回显一致。
- [ ] SEAT 才有编号，编号唯一校验生效。
- [ ] DISABLED 座位学生端不可选。

## 三、热力图实时刷新
- [ ] 首次进入获得 board_snapshot。
- [ ] 另一端预约/释放本端秒级更新对应座位。
- [ ] 断线出现提示并自动重连重取快照。

## 四、报表展示
- [ ] 报表页各图表正常渲染，按维度筛选生效。
- [ ] 无数据时空态友好。

## 五、积分排行展示【MVP+】
- [ ] 排行榜按周/月切换正确渲染。
- [ ] 学生端本人积分展示正确。

## 六、附近空位推荐展示【MVP+】
- [ ] 手动选位置返回排序列表。
- [ ] 无空位显示 `NO_AVAILABLE_ROOM_NEARBY` 文案。
- [ ] 定位失败回退手动选择。

## 七、异常场景
- [ ] 401 跳登录、403 提示无权限。
- [ ] `SEAT_ALREADY_RESERVED` 自动刷新座位。
- [ ] `DAILY_LIMIT_EXCEEDED`/`USER_IN_BLACKLIST` 文案正确。
- [ ] 网络异常可重试。

## 八、演示脚本对齐
- [ ] 与 [../docs/06-demo-script.md](../docs/06-demo-script.md) 各步骤可现场复现。

## 测试手段建议
| 类型 | 手段 |
| --- | --- |
| 组件单测 | Vitest + Vue Test Utils（SeatCell/TimePicker 校验） |
| 交互 | 手动/脚本走查关键流程 |
| SSE | 双窗口对比 + mock 事件 |
| 错误码 | mock 各错误码验证文案与跳转 |

## 实现约束
- 关键校验逻辑（时间片换算/编号唯一）应有单测。

## 验收标准
- 本文件勾选项 + [../docs/07](../docs/07-acceptance-checklist.md) 前端相关项全过。

## 给 AI Coding Agent 的提示
实现每个页面后按对应勾选项自测；异常路径不可省略。
