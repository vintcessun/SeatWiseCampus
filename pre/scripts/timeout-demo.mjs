// 下层 · 超时释放 + 爽约 + 黑名单：预约当前时段但不签到 → 到期自动释放（EXPIRED_RELEASED）
// → no_show_count+1 → 累计达阈值进黑名单 → 再预约被拒 USER_IN_BLACKLIST。
//
// ⚠️ 正常后端签到窗口为 15 分钟，无法现场压缩。请连「短签到窗口」临时后端（见 RUN.md）：
//   docker run -d --name seatwise-backend-tmp --network seatwisecampus_default \
//     -e MYSQL_HOST=mysql -e MYSQL_PASSWORD=seatwise123 -e MYSQL_DB=seatwise -e REDIS_HOST=redis \
//     -e SEATWISE_SIGNIN_WINDOW_MINUTES=0 -p 18081:8080 seatwisecampus-backend
// 然后： BASE=http://localhost:18081 node timeout-demo.mjs
//   演示后： docker rm -f seatwise-backend-tmp
//
// 注意：本脚本直接打后端端口（默认 18081），不经 nginx；BASE 需含协议与端口。
import {
  ok, today, banner, step, good, bad, info, sleep, isMain,
} from './lib.mjs'

// 本脚本默认后端直连端口 18081（短窗口临时后端）
const TBASE = process.env.BASE || 'http://localhost:18081'
async function tapi(path, opt = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (opt.token) headers['satoken'] = opt.token
  const res = await fetch(TBASE + path, { method: opt.method || 'GET', headers, body: opt.body ? JSON.stringify(opt.body) : undefined })
  return res.json().catch(() => ({}))
}
const tlogin = async (u, p) => (await tapi('/api/auth/login', { method: 'POST', body: { username: u, password: p } })).data?.token

export async function timeoutDemo(opts = {}) {
  banner('超时释放 + 黑名单 timeout-demo')
  info(`后端直连 BASE=${TBASE}（需 SEATWISE_SIGNIN_WINDOW_MINUTES=0 的临时后端）`)
  // 探活
  const ping = await tapi('/api/auth/login', { method: 'POST', body: { username: 'student8', password: '123456' } })
  if (!ok(ping)) { bad(`无法连接短窗口后端（${TBASE}）。请先启动临时后端，见本文件顶部注释。`); return {} }

  const who = opts.as || process.env.AS || 'student8' // 用高位学生，避免影响主演示账号
  const token = await tlogin(who, '123456')
  const roomId = opts.roomId || Number(process.env.ROOM) || (await firstRoomIdT(token))

  // 选当前可预约的近端时段（今天、下一格）
  const now = new Date()
  const startMin = Math.max(8 * 60, (now.getHours() * 60 + now.getMinutes())) // 立即开始（窗口=0 会马上过期）
  const s = fmt(startMin), e = fmt(startMin + 60)
  const date = today()
  const bd = await boardT(roomId, { date, start: s, end: e }, token)
  const seat = (bd.seats || []).find((x) => x.cellType === 'SEAT' && x.status === 'FREE')
  if (!seat) { bad('无空位'); return {} }

  step(`${who} 预约 ${seat.seatNo}（${s}-${e}）但不签到…`)
  const r = await tapi('/api/reservations', { method: 'POST', token, body: { roomId, seatId: seat.seatId, date, startTime: s, endTime: e } })
  if (r.code !== '0') { bad(`预约失败：${r.code}（窗口=0 时可能立即判超时，属正常，可重试）`); return {} }

  step('等待定时任务判定超时释放（约 8~12 秒）…')
  let released = false
  for (let i = 0; i < 15; i++) {
    await sleep(1000)
    const st = (await boardT(roomId, { date, start: s, end: e }, token)).seats.find((x) => x.seatId === seat.seatId)?.status
    if (st === 'FREE') { released = true; break }
  }
  good(released ? '座位已自动释放回 FREE（EXPIRED_RELEASED，no_show_count+1）' : '未观察到释放（可能窗口未到，或需换近端时段）')

  // 触发黑名单：连续爽约到阈值后再预约会被拒
  step('再次预约以观察黑名单拦截…')
  const again = await tapi('/api/reservations', { method: 'POST', token, body: { roomId, seatId: seat.seatId, date, startTime: fmt(startMin + 120), endTime: fmt(startMin + 180) } })
  if (again.code === 'USER_IN_BLACKLIST') good('已进入黑名单：再预约被拒 USER_IN_BLACKLIST（累计爽约达阈值）')
  else info(`本次未触发黑名单（code=${again.code}）——需累计到阈值（默认 3 次爽约）`)
  return { roomId, date, seatId: seat.seatId, released }
}

async function firstRoomIdT(token) { const r = await tapi('/api/study-rooms', { token }); return r.data?.[0]?.id }
async function boardT(roomId, { date, start, end }, token) {
  const q = new URLSearchParams({ date, start, end })
  return (await tapi(`/api/study-rooms/${roomId}/board?${q}`, { token })).data || { seats: [] }
}
const fmt = (min) => `${String(Math.floor(min / 60) % 24).padStart(2, '0')}:${String(min % 60).padStart(2, '0')}`

if (isMain(import.meta.url)) timeoutDemo().catch((e) => { console.error(e); process.exit(1) })
