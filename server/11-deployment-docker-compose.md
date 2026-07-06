# server/11 · Docker Compose 部署

- **文档目的**：定义本地/演示环境的容器编排与启动。
- **适用范围**：本地开发与答辩演示。
- **读者对象**：后端/运维/Agent。
- **相关文件**：[00-server-overview](00-server-overview.md)、[../docs/06-demo-script.md](../docs/06-demo-script.md)。

## 关键结论
- 一条 `docker compose up` 起 mysql/redis/backend（可选 nginx），演示零配置。

## 一、服务组成
| 服务 | 镜像 | 端口 | 说明 |
| --- | --- | --- | --- |
| mysql | mysql:8 | 3306 | 主存储，挂初始化 SQL |
| redis | redis:7 | 6379 | 缓存/锁/延迟队列 |
| backend | 自构建(JDK21) | 8080 | Spring Boot 应用 |
| nginx | nginx(可选) | 80 | 前端静态 + 反代 /api、/api/board/stream |

## 二、环境变量
| 变量 | 示例 | 用途 |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://mysql:3306/seatwise` | 数据库 |
| `SPRING_DATASOURCE_USERNAME/PASSWORD` | root/xxx | 凭据 |
| `SPRING_DATA_REDIS_HOST/PORT` | redis/6379 | Redis |
| `SA_TOKEN_*` | — | 登录态配置 |
| `SEATWISE_SIGNIN_WINDOW_MINUTES` | 15 | 签到窗口 |
| `SEATWISE_NOSHOW_THRESHOLD` | 3 | 黑名单阈值 |
| `SEATWISE_BLACKLIST_DAYS` | 7 | 黑名单期限(天) |
| `SEATWISE_DAILY_LIMIT` | 3 | 单日预约次数上限 |
| `SEATWISE_MAX_SLOTS_PER_RESERVATION` | 8 | 单次预约最大时间片数(30min×8=4h) |
| `SEATWISE_SLOT_MINUTES` | 30 | 时间片粒度(分钟) |

## 三、端口规划
| 对外 | 服务 |
| --- | --- |
| 80 | nginx(可选) |
| 8080 | backend |
| 3306/6379 | mysql/redis(仅内网/本地调试) |

## 四、数据卷
| 卷 | 用途 |
| --- | --- |
| `mysql-data` | 数据库持久化 |
| `./sql/init:/docker-entrypoint-initdb.d` | 初始化 SQL |
| `redis-data` | Redis 持久化(可选) |

## 五、初始化 SQL
- `sql/init/01-schema.sql`：建表（含 `uk_seat_date_slot`）。
- `sql/init/02-seed.sql`：种子数据（管理员/学生/校区/楼栋/自习室/座位）。
用于演示可复现（见 [../docs/06](../docs/06-demo-script.md)）。

## 六、compose 结构（示意）
```yaml
services:
  mysql:
    image: mysql:8
    environment: [MYSQL_DATABASE=seatwise, MYSQL_ROOT_PASSWORD=xxx]
    volumes: ["mysql-data:/var/lib/mysql", "./sql/init:/docker-entrypoint-initdb.d"]
  redis:
    image: redis:7
  backend:
    build: ./server
    depends_on: [mysql, redis]
    environment: [SPRING_DATASOURCE_URL=..., SPRING_DATA_REDIS_HOST=redis]
    ports: ["8080:8080"]
  # nginx: 可选
volumes: { mysql-data: {} }
```
> 以上为结构示意，落地时按实际补全。

## 七、本地启动顺序
1. `docker compose up -d mysql redis`（等待健康）。
2. 初始化 SQL 自动导入（首次）。
3. `docker compose up -d backend`。
4. 前端 `npm run dev` 或经 nginx 提供静态。

## 八、Knife4j 地址
`http://localhost:8080/doc.html`（接口调试）。

## 九、演示环境部署
- 使用 seed SQL 预置演示数据。
- 可调短签到窗口/黑名单阈值便于现场演示，演示后复原。

## 实现约束
- backend 依赖 mysql/redis 健康后启动（`depends_on` + 健康检查）。
- 初始化 SQL 幂等，重复启动不冲突。

## 验收标准
- `docker compose up` 全绿；Knife4j 可访问；前端可完成核心流程。

## 给 AI Coding Agent 的提示
生成 compose 与初始化 SQL 放 `server/` 相关目录；配置项走环境变量，勿硬编码到代码。
