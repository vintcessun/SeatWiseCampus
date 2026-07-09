# docs/00 · 文档导读

- **文档目的**：说明本项目文档如何组织与阅读，让人与 Agent 都能快速定位、避免误读。
- **适用范围**：全项目文档体系。
- **读者对象**：人类读者与 Coding Agent。
- **相关文件**：[README.md](../README.md)、[AGENTS.md](../AGENTS.md)、[CLAUDE.md](../CLAUDE.md)、[llms.txt](../llms.txt)。

## 关键结论
- 文档分三层：**入口/Agent 层**（根目录）、**通用设计层**（docs/）、**分端实现层**（client/、server/）。
- **根目录 Agent 文件只写规则与路由**，详细设计一律在 docs/ 与分端文档；不要把设计塞进 README/AGENTS/CLAUDE。
- 每份设计文档结构固定：目的/范围/读者/相关文件 → 关键结论 → 正文 → 实现约束 → 验收标准 → 给 AI 的提示。

## 一、文档分层原则
| 层 | 文件 | 作用 |
| --- | --- | --- |
| 入口/Agent | README、AGENTS、CLAUDE、llms.txt、PROJECT_CONTEXT、ROADMAP、GLOSSARY | 项目入口、规则、索引、上下文 |
| 通用设计 | docs/00–09 | 需求、架构、流程、MVP、扩展、演示、验收、已知问题 V1、已知问题 V2 |
| 前端实现 | client/00–10 + client/README + client/AGENTS | 前端设计与实现 |
| 后端实现 | server/00–13 + server/README + server/AGENTS | 后端设计与实现 |

## 二、谁优先读什么
| 对象 | 优先阅读 |
| --- | --- |
| 人类新成员 | README → PROJECT_CONTEXT → docs/01 需求 → docs/02 架构 → ROADMAP |
| Coding Agent（通用） | AGENTS.md → GLOSSARY → docs/02 → 对应端 AGENTS → 目标模块文档 |
| Claude Code | CLAUDE.md → PROJECT_CONTEXT → GLOSSARY → docs/02 → 对应端文档 |
| 答辩/演示 | docs/06 演示脚本 → docs/07 验收清单 |

## 三、MVP 必读 vs 扩展文档
| 类别 | 文档 |
| --- | --- |
| MVP 必读 | docs/01,02,03,04；server/01,02,03,04,05,06,07,08；client/00–09 |
| MVP+ | server/09（积分）、server/10（附近空位）、docs/05（对应章节） |
| 后续扩展 | docs/05（AI/通知/地图/小程序）、server/09/10 的“后续扩展”节 |

## 四、如何保持文档更新
- **先改文档，后写代码**：字段/接口/规则变更先落到 server/02、server/03、server/01。
- 前端页面/组件/store/接口调用变更同步 client/01、05、06、07。
- 每个 ROADMAP 阶段完成后更新阶段状态与相关设计文档。
- 变更术语先登记到 GLOSSARY，再全项目使用。

## 五、如何避免 Agent 误读
1. **一致性约束优先**：docs 与分端文档冲突时，以“核心一致性约束”（见 [docs/02](02-system-architecture.md) 与 AGENTS.md）为准。
2. **分级标签**：正文用 `【MVP】【MVP+】【扩展】` 标注，Agent 不得越级实现。
3. **术语以 GLOSSARY 为准**，不得自造别名。
4. recalled/记忆内容若与当前文档矛盾，以当前文档为准。

## 六、Agent 文件职责边界
| 文件 | 只约束 |
| --- | --- |
| AGENTS.md | 通用 Agent 全项目规则 |
| CLAUDE.md | Claude Code 行为 |
| llms.txt | LLM 文档索引 |
| client/AGENTS.md | **仅前端** |
| server/AGENTS.md | **仅后端** |

## 实现约束
- 新增文档遵循固定结构模板；命名沿用 `NN-kebab-title.md`。
- 单文档聚焦单主题，能被独立阅读理解。

## 验收标准
- 任一文档打开即含“目的/范围/读者/相关文件 + 关键结论”。
- 从 llms.txt 可两跳内到达任意设计文档。

## 给 AI Coding Agent 的提示
进入项目先读本文件确定路由，再按任务跳转具体设计文档；不要凭记忆假设结构。
