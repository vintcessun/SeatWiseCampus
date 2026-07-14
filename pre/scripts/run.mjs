// 主控 · 双层演示编排：起两个有头浏览器窗口（上层 Playwright），
// 在 cue 点触发 API 脚本（下层 多人/并发/背景），并在另一窗观察 SSE 联动。
//
// 用法：
//   node run.mjs                # 跑完整幕次序列（all）
//   node run.mjs reserve        # 只跑某一幕（见 pw-scenes.mjs SCENES 键名）
//   node run.mjs race           # 只演并发抢座（API 触发 + 看板观察）
//   HEADLESS=1 node run.mjs reserve   # 无头快速自检
//   DWELL=6000 SLOWMO=500 node run.mjs # 放慢节奏，便于录制/讲解
import { launchBrowser, newInjectedPage, gotoAdminBoard, countClass, waitClassAtLeast } from './pwlib.mjs'
import { SCENES } from './pw-scenes.mjs'
import { firstRoomId, login, tomorrow, banner, step, good, info, ADMIN, sleep } from './lib.mjs'
import { stage } from './stage.mjs'
import { race } from './race.mjs'
import { waitlistDemo } from './waitlist-demo.mjs'
import { groupDemo } from './group-demo.mjs'

const arg = (process.argv[2] || 'all').trim()
const D = tomorrow()
const WIN = { date: D, start: '14:00', end: '16:00' }
// 各幕使用不同时段，避免同一学生在同一时段重复预约（RESERVATION_TIME_CONFLICT）
const SLOT_RESERVE = { date: D, start: '14:00', end: '16:00' }
const SLOT_HOLD = { date: D, start: '16:00', end: '18:00' }
const SLOT_BOARD = { date: D, start: '18:00', end: '20:00' }
const RACE_WIN = { date: D, start: '20:00', end: '22:00' }

async function main() {
  banner(`SeatWise 双层演示 · 模式=${arg}`)
  const A = await launchBrowser({ label: 'A(主操作)', x: 0, y: 0 })
  const B = await launchBrowser({ label: 'B(观察/管理)', x: 964, y: 0 })
  const tok = await login('student1', '123456')
  const roomId = Number(process.env.ROOM) || (await firstRoomId(tok))
  const ctx = { roomId, win: WIN }
  info(`房间 roomId=${roomId} · 默认时段 ${WIN.start}-${WIN.end}`)

  try {
    if (arg === 'all') await runAll({ A, B }, ctx)
    else if (arg === 'race') await raceObserved({ A, B }, ctx)
    else if (SCENES[arg]) await SCENES[arg]({ A, B }, ctx)
    else { info(`未知幕次「${arg}」。可用：${Object.keys(SCENES).join(', ')}, race, all`); }
  } finally {
    step('演示结束，5 秒后关闭窗口…')
    await sleep(5000)
    await A.close().catch(() => {})
    await B.close().catch(() => {})
  }
}

// 并发抢座（可见版）：B 管理端看板观察，触发 race API 爆发
async function raceObserved({ A, B }, ctx) {
  banner('并发抢座（API 触发 + 看板观察）')
  const b = await newInjectedPage(B, ADMIN)
  await gotoAdminBoard(b, ctx.roomId, RACE_WIN)
  const before = await countClass(b, 'seat-RESERVED')
  info(`口播提示点：现在 8 个人同时抢这一个座（B 端已预约 ${before}）`)
  const r = await race({ roomId: ctx.roomId, slot: `${RACE_WIN.start}-${RACE_WIN.end}`, n: 8 })
  await waitClassAtLeast(b, 'seat-RESERVED', before + 1).catch(() => {})
  good(`并发结果：成功 ${r.success}，被拒 ${r.rejected}；B 端看板 +1 变红`)
  await sleep(Number(process.env.DWELL ?? 3500))
  await b.context().close().catch(() => {})
}

// 单幕护栏：任一幕出错只记录并继续，不中断整场预览
const guard = (label, fn) => fn().catch((e) => info(`「${label}」跳过：${e.message?.split('\n')[0]}`))

async function runAll(browsers, ctx) {
  // 0) 下层：造看板初始态
  await guard('stage', () => stage({ roomId: ctx.roomId, slot: `${SLOT_RESERVE.start}-${SLOT_RESERVE.end}` }))
  // 1-4) 上层：登录 → 预约联动 → 锁座 → 管理端看板（各用不同时段避免自冲突）
  await guard('login', () => SCENES.login(browsers))
  await guard('reserve', () => SCENES.reserve(browsers, { roomId: ctx.roomId, win: SLOT_RESERVE }))
  await guard('hold', () => SCENES.hold(browsers, { roomId: ctx.roomId, win: SLOT_HOLD }))
  await guard('board', () => SCENES.board(browsers, { roomId: ctx.roomId, win: SLOT_BOARD }))
  // 5) 并发抢座（可见）
  await guard('race', () => raceObserved(browsers, ctx))
  // 6) 候补：下层造 OFFERED → 上层前端确认（用早间独立时段，规避与上面冲突）
  await guard('waitlist-demo', () => waitlistDemo({ roomId: ctx.roomId, slot: '08:00-10:00', holder: 'student5', waiter: 'student2' }))
  await guard('waitlistAccept', () => SCENES.waitlistAccept(browsers))
  // 7) 组队原子性（下层）
  await guard('group-demo', () => groupDemo({ roomId: ctx.roomId }))
  // 8-12) 其余可见幕
  await guard('ai', () => SCENES.ai(browsers))
  await guard('spacetime', () => SCENES.spacetime(browsers, ctx))
  await guard('replay', () => SCENES.replay(browsers, ctx))
  await guard('tags', () => SCENES.tags(browsers, ctx))
  await guard('map', () => SCENES.map(browsers))
  await guard('pomodoro', () => SCENES.pomodoro(browsers))
  info('提示：超时释放/黑名单请单独运行 `node timeout-demo.mjs`（需 :18081 短窗口后端）')
}

main().catch((e) => { console.error(e); process.exit(1) })
