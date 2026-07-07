// 验证超时释放 -> 爽约计数 -> 黑名单（配合 signin-window=0 的临时后端）
const BASE = process.env.BASE || 'http://localhost:18081'

async function api(path, opt = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (opt.token) headers['satoken'] = opt.token
  const res = await fetch(BASE + path, { method: opt.method || 'GET', headers, body: opt.body ? JSON.stringify(opt.body) : undefined })
  return await res.json().catch(() => ({}))
}
async function login(u, p) { return (await api('/api/auth/login', { method: 'POST', body: { username: u, password: p } })).data?.token }
const sleep = (ms) => new Promise(r => setTimeout(r, ms))

function currentSlotWindow() {
  const now = new Date()
  const m = now.getHours() * 60 + now.getMinutes()
  const startM = Math.floor(m / 30) * 30
  const endM = startM + 30
  const fmt = (x) => String(Math.floor(x / 60) % 24).padStart(2, '0') + ':' + String(x % 60).padStart(2, '0')
  return { start: fmt(startM), end: fmt(endM) }
}

async function main() {
  let pass = 0, fail = 0
  const ok = (n, c, e = '') => { c ? (pass++, console.log(`  ✅ ${n} ${e}`)) : (fail++, console.log(`  ❌ ${n} ${e}`)) }

  const date = new Date().toISOString().slice(0, 10)
  const { start, end } = currentSlotWindow()
  const token = await login('student4', '123456')
  console.log(`student4 预约当前时段 ${start}-${end}（签到窗口=0，将立即超时）`)

  const board = (await api(`/api/study-rooms/1/board?date=${date}&start=${start}&end=${end}`, { token })).data
  const seat = board.seats.find(s => s.status === 'FREE')
  const cr = await api('/api/reservations', { method: 'POST', token, body: { roomId: 1, seatId: seat.seatId, date, startTime: start, endTime: end } })
  ok('预约成功', cr.code === '0', `status=${cr.data?.status}`)

  console.log('等待定时任务超时释放（~8s）…')
  await sleep(8000)

  const mine = (await api('/api/reservations/me', { token })).data
  const rec = mine.find(r => r.id === cr.data.id)
  ok('预约被超时释放', rec?.status === 'EXPIRED_RELEASED', `status=${rec?.status}`)

  const again = await api('/api/reservations', { method: 'POST', token, body: { roomId: 1, seatId: seat.seatId, date, startTime: start, endTime: end } })
  ok('触发黑名单后预约被拒', again.code === 'USER_IN_BLACKLIST', `code=${again.code}`)

  console.log(`\n== 超时/黑名单结果: ${pass} 通过 / ${fail} 失败 ==`)
  process.exit(fail === 0 ? 0 : 1)
}
main().catch(e => { console.error(e); process.exit(1) })
