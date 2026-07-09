// 生成含截图的 Word 文档（HTML/.doc 格式，Word 原生可打开，图片 base64 内嵌，无需第三方库）
import { readFileSync, existsSync, writeFileSync } from 'fs'
import { fileURLToPath } from 'url'
import { dirname, join } from 'path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const SHOTS = join(__dirname, 'shots')
const OUT = join(__dirname, '..', '..', 'SeatWise_进度汇报.doc')
const today = new Date().toISOString().slice(0, 10)

function img(file, cap) {
  const p = join(SHOTS, file)
  if (!existsSync(p)) return `<p style="color:#999">（截图缺失：${file}）</p>`
  const b64 = readFileSync(p).toString('base64')
  return `<div style="text-align:center;margin:14px 0">
    <img src="data:image/png;base64,${b64}" style="width:16cm;border:1px solid #ddd"/>
    <div style="color:#666;font-style:italic;font-size:10.5pt;margin-top:4px">${cap}</div>
  </div>`
}
function shot(file, title, cap) {
  return `<h3>${title}</h3>${img(file, cap)}`
}

const html = `<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word" xmlns="http://www.w3.org/TR/REC-html40">
<head><meta charset="utf-8">
<title>SeatWise Campus 进度汇报</title>
<!--[if gte mso 9]><xml><w:WordDocument><w:View>Print</w:View></w:WordDocument></xml><![endif]-->
<style>
  body{font-family:"微软雅黑",sans-serif;font-size:11pt;line-height:1.6;color:#1f2430}
  h1{color:#3b6cff;font-size:26pt;text-align:center;margin:0}
  h2{font-size:15pt;border-bottom:2px solid #3b6cff;padding-bottom:4px;margin-top:22px;color:#1a2233}
  h3{font-size:12.5pt;color:#3b3f4a;margin-top:16px}
  table{border-collapse:collapse;width:100%;font-size:10.5pt}
  th,td{border:1px solid #ccc;padding:6px 10px;text-align:left}
  th{background:#eef2ff}
  .ok{color:#1f9d55;font-weight:bold}
  ul{margin:6px 0}
  li{margin:3px 0}
  .cover{text-align:center;margin:40px 0}
  .cover .sub{font-size:18pt;font-weight:bold;margin-top:10px}
  .cover .tag{font-size:14pt;color:#666;margin-top:6px}
  .pb{page-break-before:always}
</style></head>
<body>

<div class="cover">
  <h1>SeatWise Campus</h1>
  <div class="sub">智能校园自习室预约管理平台</div>
  <div class="tag">阶段进度汇报</div>
  <p style="margin-top:26px">汇报日期：${today}　｜　版本：MVP / MVP+ 可运行版</p>
</div>

<h2>一、项目概述</h2>
<p>SeatWise Campus 是一套面向高校多校区、多楼栋、多楼层场景的 C/S 架构自习室在线预约系统，解决“空位不透明、线下占座、到场无座、使用率难统计、爽约不公平”等痛点。</p>
<ul>
  <li>学生：在线筛选自习室 → 选择时间片 → 网格选座 → 预约 → 到场签到 → 签退。</li>
  <li>管理员：维护校区/楼栋/自习室与座位排布，查看实时座位看板与统计报表，管理黑名单。</li>
  <li>公平保障：超时未签到自动释放、爽约计数与黑名单、积分激励。</li>
</ul>

<h2>二、系统架构与技术栈</h2>
<p>采用经典 C/S 架构：前端只通过 REST + SSE 与后端通信；后端负责业务规则、并发控制、权限与数据一致性；MySQL 为最终正确性来源，Redis/Redisson 负责分布式锁与缓存。</p>
<ul>
  <li><b>后端</b>：JDK 21 + Spring Boot 3.3.5 + MyBatis-Plus + MySQL 8 + Redis 7 + Redisson + Sa-Token + Knife4j。</li>
  <li><b>前端</b>：Vue 3 + Vite + Element Plus + ECharts + Pinia + Vue Router + Axios。</li>
  <li><b>部署</b>：Docker Compose 一键启动（MySQL / Redis / 后端 / 前端 nginx 四容器，含健康检查与自动初始化 SQL）。</li>
</ul>

<h2>三、已完成功能</h2>
<ul>
  <li>账号与鉴权：Sa-Token 登录、STUDENT / ADMIN 角色权限。</li>
  <li>基础数据：校区、楼栋、自习室、开放时间；座位行列网格（SEAT/AISLE/EMPTY/DISABLED）与启用禁用。</li>
  <li>核心预约：30 分钟时间片，Redisson 锁 + MySQL 唯一索引兜底，防止并发双占。</li>
  <li>座位管控：签到、签退、主动取消、超时自动释放、结束自动完成。</li>
  <li>黑名单：爽约达阈值自动进入黑名单，限制预约但不限制登录/查看。</li>
  <li>实时看板：初始化快照 + SSE 增量推送，多客户端座位状态秒级同步。</li>
  <li>数据报表：预约状态分布、热门时段、取消率/爽约率、自习室利用率排行（ECharts）。</li>
  <li>MVP+：积分排行榜、附近有空位的自习室推荐。</li>
</ul>

<h2>四、核心技术亮点</h2>
<ul>
  <li><b>并发抢座正确性</b>：预约拆分为时间片，reservation_slot 对 (seat_id,date,slot_index) 建唯一索引作为最终兜底，Redisson 锁降低冲突；即便 Redis 不可用也不会双占。</li>
  <li><b>实时一致性</b>：看板首帧拉取快照，之后仅推送 seat_reserved / seat_released / seat_in_use 增量事件，断线自动重连并重取快照。</li>
  <li><b>座位生命周期闭合</b>：待签到→（签到）使用中→（签退或到点自动完成）已完成并释放，杜绝座位卡在“使用中”。</li>
</ul>

<h2>五、测试与验证结果</h2>
<p>通过自动化脚本对系统做了端到端验证，核心红线全部通过：</p>
<table>
<tr><th>测试项</th><th>验证内容</th><th>结果</th></tr>
<tr><td>冒烟测试（13 项）</td><td>登录/看板/预约/重复预约拒绝/签到签退/座位释放/单日限次/报表/权限</td><td class="ok">13 / 13 通过</td></tr>
<tr><td>并发抢座</td><td>8 个学生同时抢同一座位同一时段</td><td class="ok">仅 1 成功、7 被拒</td></tr>
<tr><td>实时推送 SSE</td><td>订阅看板后触发预约</td><td class="ok">收到 seat_reserved</td></tr>
<tr><td>超时释放 + 黑名单</td><td>未签到超时释放并累计爽约触发黑名单</td><td class="ok">EXPIRED_RELEASED → 拒绝预约</td></tr>
<tr><td>前端页面</td><td>SPA 首页/资源/深链回退</td><td class="ok">HTTP 200</td></tr>
<tr><td>中文编码</td><td>数据库/接口/页面中文</td><td class="ok">正常显示</td></tr>
</table>

<h2>六、运行与部署</h2>
<ul>
  <li>一键启动：<code>docker compose up -d --build</code>。</li>
  <li>演示入口（前端）：http://localhost:8888　｜　接口文档 Knife4j：http://localhost:18080/doc.html。</li>
  <li>演示账号：admin / admin123（管理员），student1~8 / 123456（学生），登录页提供快捷登录。</li>
</ul>

<h2 class="pb">七、功能界面截图</h2>
${shot('01-login.png', '7.1 登录页', '登录页（学生 / 管理员，提供演示快捷登录）')}
${shot('02-student-rooms.png', '7.2 学生端 · 选座预约', '按校区 / 楼栋 / 楼层筛选自习室')}
${shot('03-student-seats.png', '7.3 学生端 · 座位选择与实时看板', '座位网格：空闲 / 已预约 / 使用中 / 不可用，实时连接')}
${shot('04-student-reservations.png', '7.4 学生端 · 我的预约', '预约记录与签到 / 签退 / 取消操作')}
${shot('05-student-nearby.png', '7.5 学生端 · 附近空位推荐', '按“同楼栋 > 距离最近 > 空位更多”推荐')}
${shot('06-student-ranking.png', '7.6 学生端 · 积分排行榜', '守约加分、爽约扣分的激励排行')}
${shot('07-admin-rooms.png', '7.7 管理端 · 自习室与座位', '自习室列表与入口')}
${shot('08-admin-layout.png', '7.8 管理端 · 座位排布编辑', '点击座位启用 / 禁用，含过道与禁用位')}
${shot('09-admin-board.png', '7.9 管理端 · 实时座位看板', '座位热力图与状态统计，多端秒级同步')}
${shot('10-admin-reports.png', '7.10 管理端 · 数据报表', '预约状态分布 / 热门时段 / 利用率排行（ECharts）')}
${shot('11-admin-blacklist.png', '7.11 管理端 · 黑名单管理', '黑名单记录与解除')}
${shot('12-knife4j.png', '7.12 后端接口文档', 'Knife4j 在线 API 文档')}

<h2 class="pb">七之二、增强功能亮点</h2>
${shot('13-ai-assistant.png', '① AI 智能选座助手（LLM）', '自然语言→意图→可解释座位推荐；接入 DeepSeek(OpenAI 兼容)，离线自动降级规则引擎')}
${shot('14-seat-hold.png', '② 临时锁座与倒计时', '点座即用 Redis TTL 保留 90 秒，SSE 广播"选择中"，他人实时可见，到期自动释放')}
${shot('15-notifications.png', '③ 站内通知中心', '积分/黑名单等事件每用户 SSE 实时推送并留存，写明原因，铃铛红点+抽屉')}
${shot('16-admin-event-feed.png', '④ 管理端实时事件流', '座位事件带时间戳滚动展示，点击定位；系统"正在实时工作"可视化')}
${shot('17-student-home.png', '⑤ 学生端首页概览', '欢迎横幅、概览卡片、待办预约、快捷操作、积分排行一屏总览')}
${shot('18-admin-home.png', '⑥ 管理端概览首页', 'KPI 卡片、预约状态分布环图、自习室实时空位、快捷入口')}
${shot('19-waitlist.png', '⑦ 候补队列（自动补位闭环）', '满员时一键候补；有人取消/超时/签退即自动保留座位 60 秒并推送通知，倒计时内一键确认，超时顺延下一位')}

<h2 class="pb">八、当前进度与里程碑</h2>
<ul>
  <li>P0 文档与脚手架：已完成（40+ 篇设计文档 + Agent 指令）。</li>
  <li>P1–P6（MVP）：登录、基础数据、座位排布、核心预约、签到/超时/黑名单、实时看板、报表 —— 已完成并可运行。</li>
  <li>P7–P8（MVP+）：积分排行、附近空位推荐 —— 已完成。</li>
  <li>大型增强（P9+）：AI 智能选座助手、临时锁座倒计时、站内通知中心、管理端实时事件流、冲突智能替代、首页概览 —— 已完成。</li>
</ul>

<h2>九、下一步计划</h2>
<ul>
  <li>将超时释放由定时扫描升级为 Redisson 延迟队列（文档既定主方案）。</li>
  <li>报表由实时聚合改为 room_daily_stats 聚合表 + 定时任务，提升大数据量性能。</li>
  <li>补充管理端基础数据的增删改表单与积分规则配置中心。</li>
  <li>引入自动化测试到 CI，扩展并发压测规模。</li>
</ul>

<h2>十、说明（务实取舍）</h2>
<ul>
  <li>为生态兼容与演示稳定，Spring Boot 采用 3.3.5；超时释放采用定时扫描（文档定义的兜底方案）；报表采用实时聚合。以上均已在代码注释与文档中标注，不影响功能演示。</li>
  <li>宿主机 8080 被占用，后端对外映射到 18080；浏览器仅需访问 8888（nginx 反代到后端）。</li>
</ul>

</body></html>`

writeFileSync(OUT, '﻿' + html, 'utf8')
console.log('Word 文档已生成 ->', OUT)
