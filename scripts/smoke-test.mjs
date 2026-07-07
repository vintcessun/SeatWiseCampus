// SeatWise Campus 冒烟 + 并发测试
// 用法: node scripts/smoke-test.mjs  (默认经前端 nginx http://localhost:8888)
const BASE = process.env.BASE || 'http://localhost:8888'

let pass = 0, fail = 0
function ok(name, cond, extra = '') {
  if (cond) { pass++; console.log(`  ✅ ${name} ${extra}`) }
  else { fail++; console.log(`  ❌ ${name} ${extra}`) }
}

async function api(path, { method = 'GET', token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['satoken'] = token
  const res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  const json = await res.json().catch(() => ({}))
  return { status: res.status, json }
}

async function login(username, password) {
  const r = await api('/api/auth/login', { method: 'POST', body: { username, password } })
  return r.json?.data?.token
}

function tomorrow() {
  const d = new Date(); d.setDate(d.getDate() + 1)
  return d.toISOString().slice(0, 10)
}

async function main() {
  console.log(`\n== SeatWise 测试 @ ${BASE} ==\n`)

  console.log('[1] 登录')
  const adminTk = await login('admin', 'admin123')
  const s1 = await login('student1', '123456')
  ok('管理员登录', !!adminTk)
  ok('学生登录', !!s1)

  console.log('[2] 基础数据')
  const rooms = (await api('/api/study-rooms', { token: s1 })).json.data
  ok('自习室列表非空', rooms?.length > 0, `(${rooms?.length} 间)`)
  const roomId = rooms[0].id
  const date = tomorrow()

  console.log('[3] 看板快照')
  const board = (await api(`/api/study-rooms/${roomId}/board?date=${date}&start=14:00&end=16:00`, { token: s1 })).json.data
  const freeSeats = board.seats.filter(s => s.status === 'FREE')
  ok('看板返回座位', board.seats.length > 0, `(${board.seats.length} 格, ${freeSeats.length} 空闲)`)
  const seatId = freeSeats[0].seatId

  console.log('[4] 提交预约')
  const create = await api('/api/reservations', { method: 'POST', token: s1, body: { roomId, seatId, date, startTime: '14:00', endTime: '16:00' } })
  ok('预约成功', create.json?.code === '0', `status=${create.json?.data?.status}`)
  const reservationId = create.json?.data?.id

  console.log('[5] 重复预约同座同时段 -> 应失败')
  const dup = await api('/api/reservations', { method: 'POST', token: await login('student2', '123456'), body: { roomId, seatId, date, startTime: '14:00', endTime: '16:00' } })
  ok('重复预约被拒', dup.json?.code === 'SEAT_ALREADY_RESERVED', `code=${dup.json?.code}`)

  console.log('[6] 并发抢座：8 个不同学生同时预约同一空闲座位 -> 只允许 1 成功')
  const board2 = (await api(`/api/study-rooms/${roomId}/board?date=${date}&start=16:00&end=18:00`, { token: s1 })).json.data
  const seat2 = board2.seats.filter(s => s.status === 'FREE')[0].seatId
  const users = ['student1', 'student2', 'student3', 'student4', 'student5', 'student6', 'student7', 'student8']
  const tokens = await Promise.all(users.map(u => login(u, '123456')))
  const results = await Promise.all(tokens.map(tk =>
    api('/api/reservations', { method: 'POST', token: tk, body: { roomId, seatId: seat2, date, startTime: '16:00', endTime: '18:00' } })
  ))
  const succ = results.filter(r => r.json?.code === '0').length
  const rejected = results.filter(r => r.json?.code === 'SEAT_ALREADY_RESERVED').length
  ok('并发仅 1 个成功', succ === 1, `(成功=${succ}, 被拒=${rejected}, 共=${results.length})`)

  console.log('[7] 签到 -> 签退')
  const ci = await api(`/api/reservations/${reservationId}/check-in`, { method: 'POST', token: s1 })
  ok('签到成功', ci.json?.data?.status === 'IN_USE', `status=${ci.json?.data?.status}`)
  const co = await api(`/api/reservations/${reservationId}/check-out`, { method: 'POST', token: s1 })
  ok('签退成功', co.json?.data?.status === 'COMPLETED', `status=${co.json?.data?.status}`)

  console.log('[8] 座位释放校验')
  const board3 = (await api(`/api/study-rooms/${roomId}/board?date=${date}&start=14:00&end=16:00`, { token: s1 })).json.data
  const seatBack = board3.seats.find(s => s.seatId === seatId)
  ok('签退后座位回到空闲', seatBack?.status === 'FREE', `status=${seatBack?.status}`)

  console.log('[9] 单日次数限制')
  const pad = (h) => String(h).padStart(2, '0') + ':00'
  let limitHit = false
  for (let h = 8; h < 14; h++) {
    const st = pad(h), en = pad(h + 1)
    const fs = (await api(`/api/study-rooms/${roomId}/board?date=${date}&start=${st}&end=${en}`, { token: s1 })).json.data
    const free = fs.seats.find(s => s.status === 'FREE')
    if (!free) continue
    const rr = await api('/api/reservations', { method: 'POST', token: s1, body: { roomId, seatId: free.seatId, date, startTime: st, endTime: en } })
    if (rr.json?.code === 'DAILY_LIMIT_EXCEEDED') { limitHit = true; break }
  }
  ok('触发单日次数上限', limitHit, '(DAILY_LIMIT_EXCEEDED)')

  console.log('[10] 报表（管理员）')
  const rep = await api('/api/reports/summary', { token: adminTk })
  ok('报表返回', rep.json?.code === '0' && rep.json.data.total >= 1, `total=${rep.json?.data?.total}`)

  console.log('[11] 权限校验：学生访问报表 -> 403')
  const denied = await api('/api/reports/summary', { token: s1 })
  ok('学生访问管理接口被拒', denied.json?.code === 'PERMISSION_DENIED', `code=${denied.json?.code}`)

  console.log(`\n== 结果: ${pass} 通过 / ${fail} 失败 ==\n`)
  process.exit(fail === 0 ? 0 : 1)
}

main().catch(e => { console.error('测试异常', e); process.exit(1) })
