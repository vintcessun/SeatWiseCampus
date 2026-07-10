// 自习室管理数据安全测试：临时关闭/开放、关闭联动通知、座位/排布未来预约校验
const BASE = 'http://localhost:8888/api'
let pass = 0, fail = 0
function ok(name, cond, extra = '') { if (cond) { pass++; console.log(`  ✓ ${name}`) } else { fail++; console.log(`  ✗ ${name} ${extra}`) } }
async function api(path, { method = 'GET', token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['satoken'] = token
  const res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  return res.json().catch(() => ({}))
}
const login = async (u, p) => (await api('/auth/login', { method: 'POST', body: { username: u, password: p } })).data.token
function tomorrow() { const d = new Date(); d.setDate(d.getDate() + 1); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` }

;(async () => {
  console.log('自习室管理数据安全测试')
  const admin = await login('admin', 'admin123')
  const date = tomorrow()
  const uname = 'roomtest_' + Date.now().toString().slice(-8)
  const reg = await api('/auth/register', { method: 'POST', body: { username: uname, password: '123456', realName: '房管测试' } })
  const stu = reg.data.token

  const roomId = (await api('/study-rooms?campusId=1', { token: stu })).data[0].id
  const seat = (await api(`/study-rooms/${roomId}/board?date=${date}&start=14:00&end=16:00`, { token: stu })).data.seats.find(s => s.status === 'FREE')
  const cr = await api('/reservations', { method: 'POST', token: stu, body: { roomId, seatId: seat.seatId, date, startTime: '14:00', endTime: '16:00' } })
  ok('学生预约成功（制造未来预约）', cr.code === '0', JSON.stringify(cr).slice(0, 100))

  // R10：停用有未来预约的座位被拒
  const tog = await api(`/seats/${seat.seatId}/toggle?enabled=0`, { method: 'POST', token: admin })
  ok('停用有未来预约的座位被拒', tog.code === 'SEAT_HAS_FUTURE_RESERVATION', JSON.stringify(tog).slice(0, 100))

  // R10：房间有未来预约时重排被拒
  const gen = await api(`/study-rooms/${roomId}/generate-layout?rows=6&cols=8&aisleCol=3`, { method: 'POST', token: admin })
  ok('有未来预约时重排座位被拒', gen.code === 'ROOM_HAS_FUTURE_RESERVATION', JSON.stringify(gen).slice(0, 100))

  // R4：关闭自习室，联动通知
  const beforeN = ((await api('/notifications', { token: stu })).data || []).length
  const close = await api(`/study-rooms/${roomId}/status?status=CLOSED`, { method: 'POST', token: admin })
  ok('临时关闭成功', close.code === '0', JSON.stringify(close).slice(0, 100))
  ok('关闭返回受影响人数≥1', close.data && close.data.affected >= 1, JSON.stringify(close.data))
  const afterN = (await api('/notifications', { token: stu })).data || []
  ok('受影响学生收到关闭通知', afterN.length > beforeN && afterN.some(n => (n.title || '').includes('关闭')), JSON.stringify(afterN.map(n => n.title)))

  // R4：关闭后不能预约
  const seat2 = (await api(`/study-rooms/${roomId}/board?date=${date}&start=16:00&end=17:00`, { token: stu })).data.seats.find(s => s.status === 'FREE')
  const bookClosed = await api('/reservations', { method: 'POST', token: stu, body: { roomId, seatId: seat2.seatId, date, startTime: '16:00', endTime: '17:00' } })
  ok('关闭状态下预约被拒（ROOM_CLOSED）', bookClosed.code === 'ROOM_CLOSED', JSON.stringify(bookClosed).slice(0, 100))

  // R4：重新开放后可预约
  const open = await api(`/study-rooms/${roomId}/status?status=OPEN`, { method: 'POST', token: admin })
  ok('重新开放成功', open.code === '0')
  const bookOpen = await api('/reservations', { method: 'POST', token: stu, body: { roomId, seatId: seat2.seatId, date, startTime: '16:00', endTime: '17:00' } })
  ok('重新开放后可预约', bookOpen.code === '0', JSON.stringify(bookOpen).slice(0, 100))

  // 正例：无未来预约的座位可正常启用/停用（用另一房间一个无预约座位）
  const rooms = (await api('/study-rooms?campusId=1', { token: stu })).data
  const other = rooms.find(r => r.id !== roomId) || rooms[0]
  const oseat = (await api(`/study-rooms/${other.id}/board?date=${date}&start=14:00&end=16:00`, { token: stu })).data.seats.find(s => s.status === 'FREE')
  const d0 = await api(`/seats/${oseat.seatId}/toggle?enabled=0`, { method: 'POST', token: admin })
  const d1 = await api(`/seats/${oseat.seatId}/toggle?enabled=1`, { method: 'POST', token: admin })
  ok('无未来预约座位可正常停用/启用', d0.code === '0' && d1.code === '0', JSON.stringify({ d0: d0.code, d1: d1.code }))

  console.log(`\n结果：${pass} 通过 / ${fail} 失败`)
  process.exit(fail ? 1 : 0)
})().catch(e => { console.error('测试异常', e); process.exit(1) })
