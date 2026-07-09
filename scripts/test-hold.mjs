const BASE = process.env.BASE || 'http://localhost:8888'
let pass = 0, fail = 0
const ok = (n, c, e = '') => { c ? (pass++, console.log(`  ✅ ${n} ${e}`)) : (fail++, console.log(`  ❌ ${n} ${e}`)) }
async function api(path, opt = {}) {
  const h = { 'Content-Type': 'application/json' }; if (opt.token) h['satoken'] = opt.token
  const r = await fetch(BASE + path, { method: opt.method || 'GET', headers: h, body: opt.body ? JSON.stringify(opt.body) : undefined })
  return await r.json().catch(() => ({}))
}
const login = async (u, p) => (await api('/api/auth/login', { method: 'POST', body: { username: u, password: p } })).data?.token
const tomorrow = () => { const d = new Date(); d.setDate(d.getDate() + 1); return d.toISOString().slice(0, 10) }

async function main() {
  console.log('\n== 临时锁座测试 ==\n')
  const s1 = await login('student1', '123456'), s2 = await login('student2', '123456')
  const T = tomorrow(), roomId = 1
  const board = (await api(`/api/study-rooms/${roomId}/board?date=${T}&start=14:00&end=16:00`, { token: s1 })).data
  const seatId = board.seats.find(s => s.status === 'FREE').seatId

  // SSE 订阅，验证 seat_hold 事件
  const events = []
  const ctrl = new AbortController()
  const stk = await login('student3', '123456')
  const resp = await fetch(`${BASE}/api/board/stream?roomId=${roomId}&date=${T}&token=${stk}`, { signal: ctrl.signal })
  const reader = resp.body.getReader(); const dec = new TextDecoder()
  ;(async () => { let buf = ''; try { while (true) { const { value, done } = await reader.read(); if (done) break; buf += dec.decode(value, { stream: true }); let i; while ((i = buf.indexOf('\n')) >= 0) { const line = buf.slice(0, i).trim(); buf = buf.slice(i + 1); if (line.startsWith('event:')) events.push(line.slice(6).trim()) } } } catch {} })()
  await new Promise(r => setTimeout(r, 600))

  console.log('[1] student1 锁座')
  const h1 = await api('/api/holds', { method: 'POST', token: s1, body: { roomId, seatId, date: T, startTime: '14:00', endTime: '16:00' } })
  ok('锁座成功并返回到期时间', h1.code === '0' && h1.data?.expireAt > Date.now(), 'holdSeconds≈' + h1.data?.holdSeconds)

  console.log('[2] 看板快照显示 HELD')
  const b2 = (await api(`/api/study-rooms/${roomId}/board?date=${T}&start=14:00&end=16:00`, { token: s2 })).data
  const held = b2.seats.find(s => s.seatId === seatId)
  ok('该座位状态为 HELD', held?.status === 'HELD', 'heldBy=' + held?.heldBy)

  console.log('[3] student2 抢锁被拒')
  const h2 = await api('/api/holds', { method: 'POST', token: s2, body: { roomId, seatId, date: T, startTime: '14:00', endTime: '16:00' } })
  ok('他人锁座被拒 SEAT_ALREADY_HELD', h2.code === 'SEAT_ALREADY_HELD', 'code=' + h2.code)

  console.log('[4] SSE 收到 seat_hold')
  await new Promise(r => setTimeout(r, 400))
  ok('订阅端收到 seat_hold 事件', events.includes('seat_hold'), '事件=' + events.filter(e => e !== 'heartbeat').join(','))

  console.log('[5] student1 释放锁')
  await api('/api/holds/release', { method: 'POST', token: s1, body: { roomId, seatId, date: T } })
  const b5 = (await api(`/api/study-rooms/${roomId}/board?date=${T}&start=14:00&end=16:00`, { token: s2 })).data
  ok('释放后回到 FREE', b5.seats.find(s => s.seatId === seatId)?.status === 'FREE')
  ok('SSE 收到 hold_released', events.includes('hold_released'))

  console.log('[6] 锁座后可确认预约，锁被清理')
  await api('/api/holds', { method: 'POST', token: s1, body: { roomId, seatId, date: T, startTime: '14:00', endTime: '16:00' } })
  const cr = await api('/api/reservations', { method: 'POST', token: s1, body: { roomId, seatId, date: T, startTime: '14:00', endTime: '16:00' } })
  ok('预约成功', cr.code === '0')
  const b6 = (await api(`/api/study-rooms/${roomId}/board?date=${T}&start=14:00&end=16:00`, { token: s2 })).data
  ok('座位变 RESERVED（锁已清理）', b6.seats.find(s => s.seatId === seatId)?.status === 'RESERVED')

  ctrl.abort()
  console.log(`\n== 结果: ${pass} 通过 / ${fail} 失败 ==\n`)
  process.exit(fail === 0 ? 0 : 1)
}
main().catch(e => { console.error(e); process.exit(1) })
