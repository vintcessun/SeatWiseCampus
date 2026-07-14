// 下层 · 场景造数：把某房间某日某时段铺成「有故事」的看板初始态（多状态座位）。
// 幂等友好：只新增，不依赖干净库；反复跑会叠加更多状态（受每日限次约束）。
// 用法： node stage.mjs            （默认 房间=首个, 日期=明天, 时段=14:00-16:00）
//        ROOM=1 DATE=2026-07-16 SLOT=14:00-16:00 N_RESERVED=3 N_INUSE=1 N_DONE=1 N_CANCELLED=1 node stage.mjs
import {
  login, firstRoomId, freeSeats, reserve, checkIn, checkOut, cancel,
  tomorrow, banner, step, good, info, isMain, STUDENTS,
} from './lib.mjs'

export async function stage(opts = {}) {
  const env = process.env
  const date = opts.date || env.DATE || tomorrow()
  const [start, end] = (opts.slot || env.SLOT || '14:00-16:00').split('-')
  const nReserved = num(opts.nReserved ?? env.N_RESERVED, 3)
  const nInUse = num(opts.nInUse ?? env.N_INUSE, 1)
  const nDone = num(opts.nDone ?? env.N_DONE, 1)
  const nCancelled = num(opts.nCancelled ?? env.N_CANCELLED, 1)

  banner('场景造数 stage')
  // 登录一批学生（每人每日限 3 次，轮换使用）
  const toks = []
  for (const s of STUDENTS) toks.push(await login(s.username, s.password))
  const roomId = opts.roomId || Number(env.ROOM) || (await firstRoomId(toks[0]))
  info(`房间=${roomId} 日期=${date} 时段=${start}-${end}`)

  const win = { date, start, end }
  const need = nReserved + nInUse + nDone + nCancelled
  const seats = await freeSeats(roomId, win, toks[0], need + 4)
  if (seats.length < need) info(`可用空位 ${seats.length} < 需求 ${need}，将尽力而为`)

  let si = 0, ui = 0
  const nextTok = () => toks[ui++ % toks.length]

  // 待签到（含发起人 student1 的一条，便于前端展示"我的预约"）
  const reserved = await make(nReserved, 'RESERVED 待签到', async () => {
    const seat = seats[si++]; if (!seat) return false
    const r = await reserve(nextTok(), { roomId, seatId: seat.seatId, ...win })
    return r.code === '0'
  })

  // 使用中（预约 + 签到）—— 注意：需签到窗口已开放，未来时段会被 SIGN_IN_TOO_EARLY 拦截
  const inUse = await make(nInUse, 'IN_USE 使用中（需签到窗口开放）', async () => {
    const seat = seats[si++]; if (!seat) return false
    const tk = nextTok()
    const r = await reserve(tk, { roomId, seatId: seat.seatId, ...win })
    if (r.code !== '0') return false
    const ci = await checkIn(tk, r.data.id)
    if (ci.code !== '0') info(`签到未成功（${ci.code}）——未来时段属正常，看板仍显示"待签到"`)
    return true
  })

  // 已完成（预约+签到+签退）—— 同样受签到窗口限制
  const done = await make(nDone, 'COMPLETED 已完成（需签到窗口开放）', async () => {
    const seat = seats[si++]; if (!seat) return false
    const tk = nextTok()
    const r = await reserve(tk, { roomId, seatId: seat.seatId, ...win })
    if (r.code !== '0') return false
    const ci = await checkIn(tk, r.data.id)
    if (ci.code === '0') await checkOut(tk, r.data.id)
    return true
  })

  // 已取消
  const cancelled = await make(nCancelled, 'CANCELLED 已取消', async () => {
    const seat = seats[si++]; if (!seat) return false
    const tk = nextTok()
    const r = await reserve(tk, { roomId, seatId: seat.seatId, ...win })
    if (r.code !== '0') return false
    await cancel(tk, r.data.id)
    return true
  })

  good(`造数完成：待签到 ${reserved}，使用中 ${inUse}，已完成 ${done}，已取消 ${cancelled}`)
  return { roomId, date, start, end, reserved, inUse, done, cancelled }
}

async function make(n, label, fn) {
  let c = 0
  for (let i = 0; i < n; i++) { step(`${label} #${i + 1}`); if (await fn()) c++ }
  return c
}
const num = (v, d) => (v == null || v === '' ? d : Number(v))

if (isMain(import.meta.url)) stage().catch((e) => { console.error(e); process.exit(1) })
