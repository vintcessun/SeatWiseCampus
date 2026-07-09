// 候补队列闭环测试：加入候补 → 座位释放自动保留 → 确认预约
const BASE = 'http://localhost:8888/api'
let pass = 0, fail = 0
function ok(name, cond, extra = '') { if (cond) { pass++; console.log(`  ✓ ${name}`) } else { fail++; console.log(`  ✗ ${name} ${extra}`) } }

async function api(path, { method = 'GET', token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['satoken'] = token
  const res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  const json = await res.json().catch(() => ({}))
  return json
}
async function login(username, password) {
  const r = await api('/auth/login', { method: 'POST', body: { username, password } })
  if (r.code !== '0') throw new Error('login failed ' + username + ' ' + JSON.stringify(r))
  return r.data.token
}
function tomorrow() {
  const d = new Date(); d.setDate(d.getDate() + 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

(async () => {
  console.log('候补队列闭环测试')
  const date = tomorrow(), start = '14:00', end = '16:00'
  const tokenA = await login('student1', '123456')
  const tokenB = await login('student2', '123456')

  const campuses = await api('/campuses', { token: tokenA })
  const campusId = campuses.data[0].id
  const rooms = await api(`/study-rooms?campusId=${campusId}`, { token: tokenA })
  const roomId = rooms.data[0].id
  console.log(`  房间 roomId=${roomId} 日期=${date} 时段=${start}-${end}`)

  const board = await api(`/study-rooms/${roomId}/board?date=${date}&start=${start}&end=${end}`, { token: tokenA })
  const seats = board.data.seats || []
  const freeSeat = seats.find(s => s.status === 'FREE')
  ok('存在空闲座位', !!freeSeat)
  const seatId = freeSeat.seatId

  // A 预约该座位
  const ra = await api('/reservations', { method: 'POST', token: tokenA, body: { roomId, seatId, date, startTime: start, endTime: end } })
  ok('A 预约成功', ra.code === '0', JSON.stringify(ra))
  const resId = ra.data?.id

  // B 加入候补
  const wj = await api('/waitlist', { method: 'POST', token: tokenB, body: { roomId, date, startTime: start, endTime: end } })
  ok('B 加入候补成功', wj.code === '0', JSON.stringify(wj))

  let mine = await api('/waitlist/me', { token: tokenB })
  ok('B 候补状态 WAITING', mine.data?.[0]?.status === 'WAITING', JSON.stringify(mine.data?.[0]))

  // A 取消 → 触发候补自动保留
  const cancel = await api(`/reservations/${resId}/cancel`, { method: 'POST', token: tokenA })
  ok('A 取消预约成功', cancel.code === '0', JSON.stringify(cancel))

  // 等待事件处理
  await new Promise(r => setTimeout(r, 800))
  mine = await api('/waitlist/me', { token: tokenB })
  const offer = mine.data?.[0]
  ok('B 候补被自动保留 OFFERED', offer?.status === 'OFFERED', JSON.stringify(offer))
  ok('保留的是被释放的座位', offer?.offeredSeatId === seatId, `offered=${offer?.offeredSeatId} expect=${seatId}`)

  // B 通知里应有 WAITLIST
  const notis = await api('/notifications', { token: tokenB })
  ok('B 收到候补通知', (notis.data || []).some(n => n.type === 'WAITLIST'), JSON.stringify((notis.data||[]).map(n=>n.type)))

  // B 确认候补 → 生成预约
  const acc = await api(`/waitlist/${offer.id}/accept`, { method: 'POST', token: tokenB })
  ok('B 确认候补成功', acc.code === '0', JSON.stringify(acc))

  const bres = await api('/reservations/me', { token: tokenB })
  ok('B 名下已有该座位预约', (bres.data || []).some(r => r.seatId === seatId && r.date === date), JSON.stringify((bres.data||[]).map(r=>({seat:r.seatId,st:r.status}))))

  mine = await api('/waitlist/me', { token: tokenB })
  ok('B 候补记录变为 FULFILLED', mine.data?.[0]?.status === 'FULFILLED', JSON.stringify(mine.data?.[0]?.status))

  console.log(`\n结果：${pass} 通过 / ${fail} 失败`)
  process.exit(fail ? 1 : 0)
})().catch(e => { console.error('测试异常', e); process.exit(1) })
