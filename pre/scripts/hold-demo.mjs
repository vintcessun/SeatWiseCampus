// 下层 · 临时锁座联动：一名学生锁座（Redis TTL 90s），产生 SSE seat_hold；
// 供另一窗口/看板观察座位变黄"选择中"。可选自动释放。
// 用法： node hold-demo.mjs                （student1 锁一个空位，保持 8 秒后释放）
//        AS=student2 KEEP=12 RELEASE=0 node hold-demo.mjs   （不自动释放，留给前端确认）
import {
  login, firstRoomId, freeSeats, hold, releaseHold, tomorrow,
  banner, step, good, bad, info, sleep, isMain,
} from './lib.mjs'

export async function holdDemo(opts = {}) {
  const env = process.env
  const date = opts.date || env.DATE || tomorrow()
  const [start, end] = (opts.slot || env.SLOT || '14:00-16:00').split('-')
  const as = opts.as || env.AS || 'student1'
  const keepSec = num(opts.keep ?? env.KEEP, 8)
  const doRelease = String(opts.release ?? env.RELEASE ?? '1') !== '0'

  banner(`临时锁座 hold-demo · ${as}`)
  const token = await login(as, '123456')
  const roomId = opts.roomId || Number(env.ROOM) || (await firstRoomId(token))
  const win = { date, start, end }

  let seatId = opts.seatId || (env.SEAT_ID && Number(env.SEAT_ID))
  let seatNo = ''
  if (!seatId) {
    const fs = await freeSeats(roomId, win, token, 1)
    if (!fs.length) { bad('无空位可锁'); return {} }
    seatId = fs[0].seatId; seatNo = fs[0].seatNo
  }
  step(`${as} 锁定座位 ${seatNo || seatId}（房间 ${roomId} · ${start}-${end}）`)
  const h = await hold(token, { roomId, seatId, ...win })
  if (h.code !== '0') { bad(`锁座失败：${h.code}`); return {} }
  good(`已锁定，保留 ${h.data.holdSeconds}s（其他端此刻看到黄色"🔒 选择中"）`)

  await sleep(keepSec * 1000)
  if (doRelease) {
    await releaseHold(token, { roomId, seatId, date })
    info('已释放临时锁（其他端座位恢复空闲）')
  } else {
    info('保持锁定未释放（可由前端窗口点击"确认预约"落库，或等其 90s 到期）')
  }
  return { roomId, seatId, seatNo, date, start, end }
}

const num = (v, d) => (v == null || v === '' ? d : Number(v))
if (isMain(import.meta.url)) holdDemo().catch((e) => { console.error(e); process.exit(1) })
