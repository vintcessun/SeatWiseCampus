// 生成含截图的 Word 进度汇报：SeatWise_进度汇报.docx
import {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType,
  ImageRun, Table, TableRow, TableCell, WidthType, BorderStyle
} from 'docx'
import { readFileSync, existsSync, writeFileSync } from 'fs'
import { fileURLToPath } from 'url'
import { dirname, join } from 'path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const SHOTS = join(__dirname, 'shots')
const OUT = join(__dirname, '..', '..', 'SeatWise_进度汇报.docx')
const FONT = '微软雅黑'

function pngSize(buf) {
  return { w: buf.readUInt32BE(16), h: buf.readUInt32BE(20) }
}
function H(text, level = HeadingLevel.HEADING_1) {
  return new Paragraph({ heading: level, spacing: { before: 240, after: 120 },
    children: [new TextRun({ text, font: FONT, bold: true })] })
}
function P(text, opt = {}) {
  return new Paragraph({ spacing: { after: 80 }, ...opt,
    children: [new TextRun({ text, font: FONT, size: opt.size || 22 })] })
}
function Bullet(text) {
  return new Paragraph({ bullet: { level: 0 }, spacing: { after: 40 },
    children: [new TextRun({ text, font: FONT, size: 22 })] })
}
function caption(text) {
  return new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 },
    children: [new TextRun({ text, font: FONT, italics: true, size: 20, color: '666666' })] })
}
function image(file) {
  const path = join(SHOTS, file)
  if (!existsSync(path)) return P(`（截图缺失：${file}）`)
  const buf = readFileSync(path)
  const { w, h } = pngSize(buf)
  const targetW = 600
  const scale = targetW / w
  return new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 120, after: 20 },
    children: [new ImageRun({ type: 'png', data: buf, transformation: { width: targetW, height: Math.round(h * scale) } })] })
}
function shotBlock(file, title, cap) {
  return [H(title, HeadingLevel.HEADING_3), image(file), caption(cap)]
}

function testTable(rows) {
  const border = { style: BorderStyle.SINGLE, size: 4, color: 'CCCCCC' }
  const borders = { top: border, bottom: border, left: border, right: border }
  const cell = (t, bold = false, color) => new TableCell({
    borders, margins: { top: 60, bottom: 60, left: 100, right: 100 },
    children: [new Paragraph({ children: [new TextRun({ text: t, font: FONT, size: 20, bold, color })] })]
  })
  const header = new TableRow({ tableHeader: true, children: [cell('测试项', true), cell('验证内容', true), cell('结果', true)] })
  const body = rows.map(r => new TableRow({ children: [cell(r[0]), cell(r[1]), cell(r[2], true, '1F9D55')] }))
  return new Table({ width: { size: 100, type: WidthType.PERCENTAGE }, rows: [header, ...body] })
}

const today = new Date().toISOString().slice(0, 10)

const children = [
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 800, after: 120 },
    children: [new TextRun({ text: 'SeatWise Campus', font: FONT, bold: true, size: 56, color: '3B6CFF' })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 80 },
    children: [new TextRun({ text: '智能校园自习室预约管理平台', font: FONT, bold: true, size: 36 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 600 },
    children: [new TextRun({ text: '阶段进度汇报', font: FONT, size: 28, color: '666666' })] }),
  new Paragraph({ alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: `汇报日期：${today}　｜　版本：MVP / MVP+ 可运行版`, font: FONT, size: 22 })] }),
  new Paragraph({ pageBreakBefore: true, children: [] }),

  H('一、项目概述'),
  P('SeatWise Campus 是一套面向高校多校区、多楼栋、多楼层场景的 C/S 架构自习室在线预约系统，解决“空位不透明、线下占座、到场无座、使用率难统计、爽约不公平”等痛点。'),
  Bullet('学生：在线筛选自习室 → 选择时间片 → 网格选座 → 预约 → 到场签到 → 签退。'),
  Bullet('管理员：维护校区/楼栋/自习室与座位排布，查看实时座位看板与统计报表，管理黑名单。'),
  Bullet('公平保障：超时未签到自动释放、爽约计数与黑名单、积分激励。'),

  H('二、系统架构与技术栈'),
  P('采用经典 C/S 架构：前端只通过 REST + SSE 与后端通信，后端负责业务规则、并发控制、权限与数据一致性；MySQL 为最终正确性来源，Redis/Redisson 负责锁与缓存。'),
  Bullet('后端：JDK 21 + Spring Boot 3.3.5 + MyBatis-Plus + MySQL 8 + Redis 7 + Redisson + Sa-Token + Knife4j。'),
  Bullet('前端：Vue 3 + Vite + Element Plus + ECharts + Pinia + Vue Router + Axios。'),
  Bullet('部署：Docker Compose 一键启动（MySQL / Redis / 后端 / 前端 nginx 四容器，含健康检查与自动初始化 SQL）。'),

  H('三、已完成功能'),
  Bullet('账号与鉴权：Sa-Token 登录、STUDENT / ADMIN 角色权限。'),
  Bullet('基础数据：校区、楼栋、自习室、开放时间；座位行列网格（SEAT/AISLE/EMPTY/DISABLED）与启用禁用。'),
  Bullet('核心预约：30 分钟时间片，Redisson 锁 + MySQL 唯一索引兜底，防止并发双占。'),
  Bullet('座位管控：签到、签退、主动取消、超时自动释放、结束自动完成。'),
  Bullet('黑名单：爽约达阈值自动进入黑名单，限制预约但不限制登录/查看。'),
  Bullet('实时看板：初始化快照 + SSE 增量推送，多客户端座位状态秒级同步。'),
  Bullet('数据报表：预约状态分布、热门时段、取消率/爽约率、自习室利用率排行（ECharts）。'),
  Bullet('MVP+：积分排行榜、附近有空位的自习室推荐。'),

  H('四、核心技术亮点'),
  Bullet('并发抢座正确性：预约拆分为时间片，reservation_slot 对 (seat_id,date,slot_index) 建唯一索引作为最终兜底，Redisson 锁降低冲突；即便 Redis 不可用也不会双占。'),
  Bullet('实时一致性：看板首帧拉取快照，之后仅推送 seat_reserved / seat_released / seat_in_use 等增量事件，断线自动重连并重取快照。'),
  Bullet('座位生命周期闭合：待签到→（签到）使用中→（签退或到点自动完成）已完成并释放，杜绝座位卡在“使用中”。'),

  H('五、测试与验证结果'),
  P('通过自动化脚本对系统做了端到端验证，核心红线全部通过：'),
  testTable([
    ['冒烟测试（13 项）', '登录/看板/预约/重复预约拒绝/签到签退/座位释放/单日限次/报表/权限', '13 / 13 通过'],
    ['并发抢座', '8 个学生同时抢同一座位同一时段', '仅 1 成功、7 被拒'],
    ['实时推送 SSE', '订阅看板后触发预约', '收到 seat_reserved'],
    ['超时释放 + 黑名单', '未签到超时释放并累计爽约触发黑名单', 'EXPIRED_RELEASED → 拒绝预约'],
    ['前端页面', 'SPA 首页/资源/深链回退', 'HTTP 200'],
    ['中文编码', '数据库/接口/页面中文', '正常显示'],
  ]),

  H('六、运行与部署'),
  Bullet('一键启动：docker compose up -d --build。'),
  Bullet('演示入口（前端）：http://localhost:8888　｜　接口文档 Knife4j：http://localhost:18080/doc.html。'),
  Bullet('演示账号：admin / admin123（管理员），student1~8 / 123456（学生），登录页提供快捷登录。'),

  H('七、功能界面截图'),
  ...shotBlock('01-login.png', '7.1 登录页', '登录页（学生 / 管理员，提供演示快捷登录）'),
  ...shotBlock('02-student-rooms.png', '7.2 学生端 · 选座预约', '按校区 / 楼栋 / 楼层筛选自习室'),
  ...shotBlock('03-student-seats.png', '7.3 学生端 · 座位选择与实时看板', '座位网格：空闲 / 已预约 / 使用中 / 不可用，实时连接'),
  ...shotBlock('04-student-reservations.png', '7.4 学生端 · 我的预约', '预约记录与签到 / 签退 / 取消操作'),
  ...shotBlock('05-student-nearby.png', '7.5 学生端 · 附近空位推荐', '按“同楼栋 > 距离最近 > 空位更多”推荐'),
  ...shotBlock('06-student-ranking.png', '7.6 学生端 · 积分排行榜', '守约加分、爽约扣分的激励排行'),
  ...shotBlock('07-admin-rooms.png', '7.7 管理端 · 自习室与座位', '自习室列表与入口'),
  ...shotBlock('08-admin-layout.png', '7.8 管理端 · 座位排布编辑', '点击座位启用 / 禁用，含过道与禁用位'),
  ...shotBlock('09-admin-board.png', '7.9 管理端 · 实时座位看板', '座位热力图与状态统计，多端秒级同步'),
  ...shotBlock('10-admin-reports.png', '7.10 管理端 · 数据报表', '预约状态分布 / 热门时段 / 利用率排行（ECharts）'),
  ...shotBlock('11-admin-blacklist.png', '7.11 管理端 · 黑名单管理', '黑名单记录与解除'),
  ...shotBlock('12-knife4j.png', '7.12 后端接口文档', 'Knife4j 在线 API 文档'),

  H('八、当前进度与里程碑'),
  Bullet('P0 文档与脚手架：已完成（含 40+ 篇设计文档 + Agent 指令）。'),
  Bullet('P1-P6（MVP）：登录、基础数据、座位排布、核心预约、签到/超时/黑名单、实时看板、报表 —— 已完成并可运行。'),
  Bullet('P7-P8（MVP+）：积分排行、附近空位推荐 —— 已完成。'),
  Bullet('P9（后续）：AI 推荐、通知提醒、校园地图 —— 仅接口/文档预留，未实现。'),

  H('九、下一步计划'),
  Bullet('将超时释放由定时扫描升级为 Redisson 延迟队列（文档既定主方案）。'),
  Bullet('报表由实时聚合改为 room_daily_stats 聚合表 + 定时任务，提升大数据量性能。'),
  Bullet('补充管理端基础数据的增删改表单与积分规则配置中心。'),
  Bullet('引入自动化测试到 CI，扩展并发压测规模。'),

  H('十、说明（务实取舍）'),
  Bullet('为生态兼容与演示稳定，Spring Boot 采用 3.3.5；超时释放采用定时扫描（文档定义的兜底方案）；报表采用实时聚合。以上均已在代码注释与文档中标注，不影响功能演示。'),
  Bullet('宿主机 8080 被占用，后端对外映射到 18080；浏览器仅需访问 8888（nginx 反代到后端）。'),
]

const doc = new Document({
  styles: { default: { document: { run: { font: FONT, size: 22 } } } },
  sections: [{ properties: {}, children }]
})

const buffer = await Packer.toBuffer(doc)
writeFileSync(OUT, buffer)
console.log('Word 文档已生成 ->', OUT)
