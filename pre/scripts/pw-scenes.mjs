// 上层 · Playwright 演示幕次（可见叙事）。每一幕独立、可单独运行，也可由 run.mjs 串联。
// 约定：browsers = { A, B }（两个有头浏览器窗口）。默认 A=主操作学生，B=辅助/管理端。
// 每一幕结束保留 DWELL 毫秒供录制/讲解。
import {
  newInjectedPage, gotoSeats, gotoAdminBoard, gotoPath, formLogin,
  clickFirstFreeSeat, confirmReserve, freeSeatLoc, countClass, waitClassAtLeast, sleep,
} from './pwlib.mjs'
import { banner, step, good, bad, info, ADMIN, today, hhmm } from './lib.mjs'

// 今天的一个未来 2 小时时段（对齐 30 分钟片，晚于当前时间，且不超过开放 22:00）
function futureSlotToday() {
  const now = new Date()
  let s = Math.ceil((now.getHours() * 60 + now.getMinutes()) / 30) * 30 + 30
  if (s + 120 > 22 * 60) s = 22 * 60 - 120
  if (s < 8 * 60) s = 8 * 60
  return { date: today(), start: hhmm(s), end: hhmm(s + 120) }
}

const S1 = { username: 'student1', password: '123456' }
const S2 = { username: 'student2', password: '123456' }
const S3 = { username: 'student3', password: '123456' }
const S7 = { username: 'student7', password: '123456' }
const S8 = { username: 'student8', password: '123456' }
export const DWELL = Number(process.env.DWELL ?? 3500)
const dwell = (ms = DWELL) => sleep(ms)

async function closeCtx(page) { try { await page.context().close() } catch {} }

// ── 幕 1：可见登录（两窗真实表单登录）───────────────────────────────
export async function sceneLogin({ A, B }) {
  banner('幕1 · 登录（可见表单）')
  const a = await A.newContext({ viewport: null }).then((c) => c.newPage())
  const b = await B.newContext({ viewport: null }).then((c) => c.newPage())
  step('窗口A 学生1 登录'); await formLogin(a, S1.username, S1.password); good('A 进入学生端')
  step('窗口B 学生2 登录'); await formLogin(b, S2.username, S2.password); good('B 进入学生端')
  await dwell()
  await closeCtx(a); await closeCtx(b)
}

// ── 幕 2：预约闭环 + 双窗看板联动（A 预约 → B 观察变红）──────────────
export async function sceneReserve({ A, B }, { roomId, win }) {
  banner('幕2 · 选座预约 + 双窗看板联动')
  const a = await newInjectedPage(A, S7) // 用 student7（stage 未占用该时段），避免自冲突
  const b = await newInjectedPage(B, S2)
  await gotoSeats(a, roomId, win)
  await gotoSeats(b, roomId, win)
  const before = await countClass(b, 'seat-RESERVED')
  info(`B 端当前已预约 ${before} 座`)
  step('A：点空位 → 确认预约')
  await clickFirstFreeSeat(a)
  await confirmReserve(a)
  good('A 预约成功（彩带）')
  step('B：观察该座位秒级变红（SSE seat_reserved）')
  await waitClassAtLeast(b, 'seat-RESERVED', before + 1)
  good(`B 端已预约 ${await countClass(b, 'seat-RESERVED')} 座（+1，实时同步）`)
  await dwell()
  await closeCtx(a); await closeCtx(b)
}

// ── 幕 3：临时锁座联动（A 点座保持锁定 → B 观察变黄"选择中"）──────────
export async function sceneHold({ A, B }, { roomId, win }) {
  banner('幕3 · 临时锁座（A 锁 / B 看黄）')
  const a = await newInjectedPage(A, S3) // 用 student3，避免与预约幕的 student1 抢配额
  const b = await newInjectedPage(B, S2)
  await gotoSeats(a, roomId, win)
  await gotoSeats(b, roomId, win)
  const before = await countClass(b, 'seat-HELD')
  step('A：点一个空位（触发 90s 临时锁，保持对话框打开）')
  await clickFirstFreeSeat(a)
  await page_waitDialog(a)
  step('B：观察座位变黄「🔒 选择中」（SSE seat_hold）')
  await waitClassAtLeast(b, 'seat-HELD', before + 1)
  good(`B 端出现 ${await countClass(b, 'seat-HELD')} 个"选择中"座位`)
  await dwell()
  step('A：关闭对话框（未确认）→ 锁释放，座位回绿')
  await a.getByRole('button', { name: '取消' }).first().click().catch(() => {})
  await dwell(1500)
  await closeCtx(a); await closeCtx(b)
}
const page_waitDialog = (p) => p.locator('.el-dialog:has-text("确认预约")').waitFor({ timeout: 8000 })

// ── 幕 4：管理端实时看板（A 学生预约 → B 管理端看板+事件流同步）─────────
export async function sceneBoardAdmin({ A, B }, { roomId }) {
  banner('幕4 · 管理端实时看板同步')
  // 管理端看板固定展示「今天」，SSE 按 roomId+date 分频道，故本幕预约必须落在【今天】的未来时段，
  // 事件才能到达管理端看板（其 seat_reserved 处理与窗口无关，会直接把该座位置红）。用 student8 避免占用冲突。
  const bwin = futureSlotToday()
  const a = await newInjectedPage(A, S8)
  const b = await newInjectedPage(B, ADMIN) // B 切管理员
  await gotoSeats(a, roomId, bwin)
  await gotoAdminBoard(b, roomId, { date: bwin.date, start: '14:00', end: '16:00' })
  await b.locator('.el-tag:has-text("实时连接中")').first().waitFor({ timeout: 12000 }).catch(() => {})
  const before = await countClass(b, 'seat-RESERVED')
  step(`A：预约今天 ${bwin.start}-${bwin.end} 一个座位`)
  await clickFirstFreeSeat(a)
  await confirmReserve(a)
  step('B（管理端）：看板座位秒级变红 + 右侧实时事件流滚动（SSE seat_reserved）')
  const synced = await waitClassAtLeast(b, 'seat-RESERVED', before + 1, 10000).then(() => true).catch(() => false)
  synced ? good(`管理端看板已秒级同步（已预约 ${await countClass(b, 'seat-RESERVED')} 座）`) : info('未观测到新增（该学生该时段可能已占位）')
  await dwell()
  await closeCtx(a); await closeCtx(b)
}

// ── 幕 5：智能推荐（A 打开 AI 助手 → 发送 → 可解释 Top-3）─────────────
export async function sceneAi({ A }) {
  banner('幕5 · 可解释智能推荐（AI 助手）')
  const a = await newInjectedPage(A, S1)
  await gotoPath(a, '/student/home')
  step('A：点右下角 🤖 打开助手')
  await a.locator('.ai-fab').click()
  await a.locator('.ai-panel').waitFor({ timeout: 8000 })
  step('A：点示例词发起一次推荐')
  await a.locator('.ai-chip').first().click()
  await a.locator('.ai-rec').first().waitFor({ timeout: 15000 }).catch(() => info('未返回推荐卡（检查 AI 服务/离线规则）'))
  const n = await a.locator('.ai-rec').count()
  good(`返回 ${n} 条可解释推荐（含房间/座位/标签/理由）`)
  await dwell()
  await closeCtx(a)
}

// ── 幕 6：组队相邻原子预约（A 开组队开关 → 选连续座 → 分配成员 → 提交）──
export async function sceneGroup({ A }, { roomId, win }) {
  banner('幕6 · 组队相邻原子预约')
  const a = await newInjectedPage(A, S1)
  await gotoSeats(a, roomId, win)
  step('A：打开「组队相邻预约」开关')
  await a.getByText('组队相邻预约').click()
  step('A：连续点 3 个相邻空位')
  const free = freeSeatLoc(a)
  for (let i = 0; i < 3; i++) { await free.nth(i).click().catch(() => {}) }
  info('（如未连续，成员卡可能少于 3；演示按实际可选数）')
  // 为成员卡填用户名
  const names = ['student1', 'student2', 'student3']
  const memberInputs = a.locator('.el-card:has-text("组队相邻预约") input')
  const cnt = await memberInputs.count()
  for (let i = 0; i < cnt; i++) await memberInputs.nth(i).fill(names[i] || 'student' + (i + 1))
  step('A：提交组队预约')
  await a.locator('.el-card:has-text("组队相邻预约") .el-button--primary').first().click()
  await dwell()
  await closeCtx(a)
}

// ── 幕 7：时空座位图 + 时间轴播放（A）─────────────────────────────
export async function sceneSpacetime({ A }, { win }) {
  banner('幕7 · 时空座位图 + 时间轴播放')
  const a = await newInjectedPage(A, S1)
  await gotoPath(a, '/student/spacetime')
  await a.locator('.el-slider').first().waitFor({ timeout: 12000 }).catch(() => {})
  step('A：点「播放一天」重建全天占用')
  await a.getByRole('button', { name: /播放一天|暂停/ }).first().click().catch(() => info('未找到播放按钮'))
  await dwell(5000)
  await closeCtx(a)
}

// ── 幕 8：历史回放（管理端 B/A）──────────────────────────────────
export async function sceneReplay({ A }, { roomId, win }) {
  banner('幕8 · 历史回放（管理端）')
  const a = await newInjectedPage(A, ADMIN)
  await gotoPath(a, `/admin/rooms/${roomId}/replay?date=${win.date}`)
  await a.locator('.el-slider').first().waitFor({ timeout: 12000 }).catch(() => {})
  step('A：播放回放，观察利用率仪表盘 + 曲线')
  await a.getByRole('button', { name: /播放|暂停/ }).first().click().catch(() => info('未找到播放按钮'))
  await dwell(5000)
  await closeCtx(a)
}

// ── 幕 9：座位标签编辑 + 持久化（管理端）──────────────────────────
export async function sceneTags({ A }, { roomId }) {
  banner('幕9 · 座位标签可编辑可持久化')
  const a = await newInjectedPage(A, ADMIN)
  await gotoPath(a, `/admin/rooms/${roomId}/layout`)
  await a.locator('.seat-cell').first().waitFor({ timeout: 12000 }).catch(() => {})
  step('A：开启「手动编辑模式」（显示保存按钮）')
  await a.getByText('手动编辑模式').click().catch(() => {})
  await sleep(400)
  step('A：右键一个座位，勾选标签')
  const seat = a.locator('.seat-cell.seat-cell-SEAT, .seat-cell').filter({ hasText: /A|B|\d/ }).first()
  await seat.click({ button: 'right' }).catch(() => a.locator('.seat-cell').first().click({ button: 'right' }))
  const menu = a.locator('.tag-menu')
  if (await menu.count()) {
    await menu.locator('.el-checkbox').first().click().catch(() => {})
    // 关闭菜单
    await a.locator('.tag-menu-backdrop').click().catch(() => {})
    await sleep(400)
    step('A：点「保存布局」持久化')
    const saveBtn = a.locator('.el-button--primary:has-text("保存布局")').first()
    if (await saveBtn.count()) {
      await saveBtn.click()
      await a.locator('.el-message--success').first().waitFor({ timeout: 6000 }).catch(() => {})
      good('标签已保存（刷新后仍在）')
    } else info('未找到「保存布局」按钮')
  } else info('未弹出标签菜单（该格可能非 SEAT，可换一个）')
  await dwell()
  await closeCtx(a)
}

// ── 幕 10：地图选点（管理端）────────────────────────────────────
export async function sceneMap({ A }) {
  banner('幕10 · 地图选点')
  const a = await newInjectedPage(A, ADMIN)
  await gotoPath(a, '/admin/locations')
  step('A：点某楼栋「地图选点」')
  await a.getByRole('button', { name: '地图选点' }).first().click().catch(() => info('未找到地图选点按钮'))
  const dlg = a.locator('.el-dialog:has-text("地图选点")')
  await dlg.waitFor({ timeout: 8000 }).catch(() => {})
  step('A：在地图上点选坐标')
  await dlg.locator('.leaflet-container, [class*=map]').first().click({ position: { x: 180, y: 140 } }).catch(() => {})
  await dwell(1500)
  await dlg.getByRole('button', { name: /确认坐标/ }).click().catch(() => {})
  await dlg.getByRole('button', { name: /保存坐标/ }).click().catch(() => {})
  await dwell(1500)
  await closeCtx(a)
}

// ── 幕 11：番茄钟（A，纯前端）───────────────────────────────────
export async function scenePomodoro({ A }) {
  banner('幕11 · 番茄钟')
  const a = await newInjectedPage(A, S1)
  await gotoPath(a, '/student/pomodoro')
  step('A：点「开始」专注计时')
  await a.getByRole('button', { name: /开始|暂停/ }).first().click().catch(() => info('未找到开始按钮'))
  await dwell(4000)
  await closeCtx(a)
}

// ── 幕 12：候补队列确认（B 学生2 在「我的候补」点确认）─────────────────
// 需先由 API 层 waitlist-demo 造出 OFFERED；本幕只演可见的「立即确认预约」。
export async function sceneWaitlistAccept({ B }) {
  banner('幕12 · 候补自动补位 · 前端确认')
  const b = await newInjectedPage(B, S2)
  await gotoPath(b, '/student/waitlist')
  await b.locator('.el-table').first().waitFor({ timeout: 10000 }).catch(() => {})
  step('B：在「我的候补」查看「🔒 席位已保留 Ns」')
  const accept = b.getByRole('button', { name: '立即确认预约' }).first()
  const seen = await accept.waitFor({ timeout: 8000 }).then(() => true).catch(() => false)
  if (seen) {
    await accept.click()
    await b.locator('.el-message--success').first().waitFor({ timeout: 6000 }).catch(() => {})
    good('B 确认候补 → 生成正式预约')
  } else info('当前无 OFFERED 候补（请先运行 waitlist-demo，60s 内确认）')
  await dwell()
  await closeCtx(b)
}

export const SCENES = {
  login: sceneLogin,
  reserve: sceneReserve,
  hold: sceneHold,
  board: sceneBoardAdmin,
  ai: sceneAi,
  group: sceneGroup,
  spacetime: sceneSpacetime,
  replay: sceneReplay,
  tags: sceneTags,
  map: sceneMap,
  pomodoro: scenePomodoro,
  waitlistAccept: sceneWaitlistAccept,
}
