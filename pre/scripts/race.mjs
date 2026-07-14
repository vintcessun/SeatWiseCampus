// 下层 · 并发抢座爆发：N 个不同学生在同一时刻抢同一座位，仅 1 人成功。
// 演示 Redisson 锁 + MySQL 唯一索引 uk_seat_date_slot 的最终正确性。
// 用法： node race.mjs              （默认 8 人抢 明天 16:00-18:00 的一个空位）
//        ROOM=1 DATE=2026-07-16 SLOT=16:00-18:00 N=8 node race.mjs
import {
  login, firstRoomId, freeSeats, reserve, tomorrow,
  banner, step, good, bad, info, isMain, STUDENTS,
} from './lib.mjs'

export async function race(opts = {}) {
  const env = process.env
  const date = opts.date || env.DATE || tomorrow()
  const [start, end] = (opts.slot || env.SLOT || '16:00-18:00').split('-')
  const N = Math.min(num(opts.n ?? env.N, 8), STUDENTS.length)

  banner(`并发抢座 race · ${N} 人抢 1 座`)
  const tokens = await Promise.all(STUDENTS.slice(0, N).map((s) => login(s.username, s.password)))
  const roomId = opts.roomId || Number(env.ROOM) || (await firstRoomId(tokens[0]))
  const win = { date, start, end }

  // 选定目标空位（可由 opts.seatId 指定，便于与看板镜头对齐）
  let seatId = opts.seatId || (env.SEAT_ID && Number(env.SEAT_ID))
  if (!seatId) {
    const fs = await freeSeats(roomId, win, tokens[0], 1)
    if (!fs.length) { bad('没有空位可抢，请先释放或换时段'); return { success: 0, rejected: 0 } }
    seatId = fs[0].seatId
    info(`目标座位 seatId=${seatId}（${fs[0].seatNo}） 房间=${roomId} 时段=${start}-${end}`)
  }

  step(`${N} 个请求同时发出…`)
  const t0 = Date.now()
  const results = await Promise.all(tokens.map((tk) => reserve(tk, { roomId, seatId, ...win })))
  const ms = Date.now() - t0

  const success = results.filter((r) => r.code === '0').length
  const rejected = results.filter((r) => r.code === 'SEAT_ALREADY_RESERVED').length
  const other = results.length - success - rejected
  good(`并发 ${N}：成功 ${success}，被拒 ${rejected}（SEAT_ALREADY_RESERVED）${other ? `，其它 ${other}` : ''} · 耗时 ${ms}ms`)
  if (success !== 1) bad(`期望恰好 1 个成功，实际 ${success}（可能座位已被占或库存在旧数据，换时段/座位重试）`)
  if (other) info('其它返回：' + JSON.stringify(results.filter((r) => r.code !== '0' && r.code !== 'SEAT_ALREADY_RESERVED').map((r) => r.code)))
  return { success, rejected, other, seatId, roomId, date, start, end }
}

const num = (v, d) => (v == null || v === '' ? d : Number(v))
if (isMain(import.meta.url)) race().catch((e) => { console.error(e); process.exit(1) })
