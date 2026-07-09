const BASE = process.env.BASE || 'http://localhost:8888'
let pass = 0, fail = 0
const ok = (n, c, e = '') => { c ? (pass++, console.log(`  ✅ ${n} ${e}`)) : (fail++, console.log(`  ❌ ${n} ${e}`)) }
async function api(path, opt = {}) {
  const h = { 'Content-Type': 'application/json' }; if (opt.token) h['satoken'] = opt.token
  const r = await fetch(BASE + path, { method: opt.method || 'GET', headers: h, body: opt.body ? JSON.stringify(opt.body) : undefined })
  return await r.json().catch(() => ({}))
}
const login = async (u, p) => (await api('/api/auth/login', { method: 'POST', body: { username: u, password: p } })).data?.token
const today = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` }

async function main() {
  console.log('\n== 通知中心测试 ==\n')
  const s1 = await login('student1', '123456')

  // 订阅每用户通知 SSE
  const events = []
  const ctrl = new AbortController()
  const resp = await fetch(`${BASE}/api/notifications/stream?token=${s1}`, { signal: ctrl.signal })
  const reader = resp.body.getReader(); const dec = new TextDecoder()
  ;(async () => { let buf = ''; try { while (true) { const { value, done } = await reader.read(); if (done) break; buf += dec.decode(value, { stream: true }); let i; while ((i = buf.indexOf('\n')) >= 0) { const line = buf.slice(0, i).trim(); buf = buf.slice(i + 1); if (line.startsWith('event:')) events.push(line.slice(6).trim()) } } } catch {} })()
  await new Promise(r => setTimeout(r, 600))

  const before = (await api('/api/notifications/unread-count', { token: s1 })).data.unread
  console.log('初始未读:', before)

  // 触发扣分通知：预约“临近开始(<30分钟)”的时段后立即取消 → -1
  const now = new Date()
  let m = now.getHours() * 60 + now.getMinutes()
  let startM = Math.ceil(m / 30) * 30
  if (startM <= m) startM += 30
  const pad = x => String(Math.floor(x / 60)).padStart(2, '0') + ':' + String(x % 60).padStart(2, '0')
  const st = pad(startM), en = pad(startM + 30)
  const T = today()
  console.log(`[1] 预约临近时段 ${st}-${en} 后取消（应扣1分并通知）`)
  const board = (await api(`/api/study-rooms/1/board?date=${T}&start=${st}&end=${en}`, { token: s1 })).data
  const seat = board.seats.find(s => s.status === 'FREE')
  const cr = await api('/api/reservations', { method: 'POST', token: s1, body: { roomId: 1, seatId: seat.seatId, date: T, startTime: st, endTime: en } })
  ok('预约成功', cr.code === '0', 'code=' + cr.code)
  const cc = await api(`/api/reservations/${cr.data.id}/cancel`, { method: 'POST', token: s1 })
  ok('临近取消扣1分', cc.data?.scoreDelta === -1, 'delta=' + cc.data?.scoreDelta)

  await new Promise(r => setTimeout(r, 700))
  console.log('[2] SSE 实时推送')
  ok('收到 notification 事件', events.includes('notification'), '事件=' + events.filter(e => e !== 'heartbeat').join(','))

  console.log('[3] 通知留存与未读计数')
  const list = (await api('/api/notifications', { token: s1 })).data
  ok('通知已入库', list.length >= 1, '(' + list.length + ' 条) 首条=' + list[0]?.title + ' / ' + list[0]?.content)
  const after = (await api('/api/notifications/unread-count', { token: s1 })).data.unread
  ok('未读计数增加', after > before, before + ' → ' + after)

  console.log('[4] 标记已读')
  await api(`/api/notifications/${list[0].id}/read`, { method: 'POST', token: s1 })
  const after2 = (await api('/api/notifications/unread-count', { token: s1 })).data.unread
  ok('单条已读后未读减少', after2 === after - 1, after + ' → ' + after2)
  await api('/api/notifications/read-all', { method: 'POST', token: s1 })
  ok('全部已读后为0', (await api('/api/notifications/unread-count', { token: s1 })).data.unread === 0)

  ctrl.abort()
  console.log(`\n== 结果: ${pass} 通过 / ${fail} 失败 ==\n`)
  process.exit(fail === 0 ? 0 : 1)
}
main().catch(e => { console.error(e); process.exit(1) })
