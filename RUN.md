# 运行指南 · SeatWise Campus

一条命令启动整套系统（前端 + 后端 + MySQL + Redis）。

## 前置
- Docker + Docker Compose（已验证 Docker 29 / Compose v5）

## 一键启动
```bash
docker compose up -d --build
```
首次启动会构建后端（Maven）与前端（Vite）镜像，并初始化数据库（`sql/init` 自动导入表结构与种子数据）。

## 访问地址
| 服务 | 地址 |
| --- | --- |
| 前端（演示入口） | http://localhost:8888 |
| 后端 API | http://localhost:18080 |
| 接口文档 Knife4j | http://localhost:18080/doc.html |
| MySQL | localhost:3307（root / seatwise123，库 seatwise） |
| Redis | localhost:6380 |

> 后端对宿主机映射到 **18080**（避开被占用的 8080）；前端 nginx 在容器网络内反代到后端 8080，浏览器只需访问 8888 即可。

## 演示账号
| 账号 | 密码 | 角色 |
| --- | --- | --- |
| admin | admin123 | 管理员 |
| student1 张三 | 123456 | 学生 |
| student2 李四 | 123456 | 学生 |
| student3~8 | 123456 | 学生 |

登录页提供「快捷登录」按钮，演示时一键进入。

## 演示动线（建议）
1. **实时看板同步**：两个浏览器窗口，学生端选座页与管理员实时看板打开同一自习室；一端预约，另一端座位秒级变色（SSE）。
2. **并发抢座**：运行 `node scripts/smoke-test.mjs`，观察 8 人抢同一座位仅 1 人成功。
3. **签到/签退/超时**：预约后签到→签退，座位释放；把签到窗口调短或不签到，观察定时任务自动释放并计爽约。
4. **黑名单**：student4 已接近阈值，制造爽约触发黑名单，再预约被拒。
5. **报表 & 积分**：管理员报表页 ECharts 图表；学生端积分排行。

## 冒烟 / 并发 / 实时测试
```bash
node scripts/smoke-test.mjs     # 默认经 nginx http://localhost:8888
node scripts/sse-test.mjs       # SSE 实时推送验证（订阅后触发预约收到 seat_reserved）
```
`smoke-test` 覆盖：登录、看板、预约、重复预约拒绝、8 并发仅 1 成功、签到签退、座位释放、单日限次、报表、权限（13 项全绿）。

超时释放/黑名单需要极短签到窗口，用临时后端验证（可选）：
```bash
docker run -d --name seatwise-backend-tmp --network seatwisecampus_default \
  -e MYSQL_HOST=mysql -e MYSQL_PASSWORD=seatwise123 -e MYSQL_DB=seatwise -e REDIS_HOST=redis \
  -e SEATWISE_SIGNIN_WINDOW_MINUTES=0 -p 18081:8080 seatwisecampus-backend
node scripts/timeout-test.mjs
docker rm -f seatwise-backend-tmp
```

## AI 智能选座助手（LLM）
学生端右下角 🤖 悬浮助手：一句话说需求 → 可解释座位推荐（详见 [server/14-ai-assistant-design.md](server/14-ai-assistant-design.md)）。
- **默认离线可用**：不配置密钥即走规则引擎，演示无需联网。
- **接入大模型（OpenAI 兼容）**：设置环境变量后 `docker compose up -d backend` 即启用：
```bash
export AI_BASE_URL=https://api.deepseek.com/v1   # 或 OpenAI/通义千问兼容端点
export AI_API_KEY=sk-xxxx
export AI_MODEL=deepseek-chat                     # 或 gpt-4o-mini / qwen-plus
```

## 本地开发（可选，不走容器）
- 后端：需 JDK 21 + Maven；`MYSQL_HOST/REDIS_HOST` 指向本机依赖后 `mvn spring-boot:run`。
- 前端：`cd client && npm install && npm run dev`（Vite 代理 `/api` 到 8080），访问 http://localhost:5173

## 停止 / 清理
```bash
docker compose down          # 停止
docker compose down -v       # 停止并清空数据库数据卷（重置演示数据）
```
