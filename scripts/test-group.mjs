// 组队相邻预约测试：功能闭环 + 并发原子性（两组抢重叠相邻座位，恰好一组整体成功）
const BASE = 'http://localhost:8888/api'
let pass = 0, fail = 0
function ok(name, cond, extra = '') { if (cond) { pass++; console.log(`  ✓ ${name}`) } else { fail++; console.log(`  ✗ ${name} ${extra}`) } }
async function api(path, { method = 'GET', token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['satoken'] = token
  const res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  return res.json().catch(() => ({}))
}
async function login(u, p) { const r = await api('/auth/login', { method: 'POST', body: { username: u, password: p } }); if (r.code !== '0') throw new Error('login ' + u + ' ' + JSON.stringify(r)); return r.data.token }
function tomorrow() { const d = new Date(); d.setDate(d.getDate() + 1); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` }

(async () => {
  console.log('组队相邻预约测试')
  const date = tomorrow(), start = '10:00', end = '12:00'
  const t = {}
  for (let i = 1; i <= 6; i++) t['s' + i] = await login('student' + i, '123456')

  // 找 4 个同排连续空位
  const rooms = (await api(`/study-rooms?campusId=${(await api('/campuses', { token: t.s1 })).data[0].id}`, { token: t.s1 })).data
  const roomId = rooms[0].id
  const seats = (await api(`/study-rooms/${roomId}/board?date=${date}&start=${start}&end=${end}`, { token: t.s1 })).data.seats
  const free = seats.filter(s => s.status === 'FREE' && s.cellType === 'SEAT')
  // 按行分组找连续列
  const byRow = {}
  free.forEach(s => { (byRow[s.rowIndex] ||= []).push(s) })
  let run = null
  for (const r of Object.keys(byRow)) {
    const arr = byRow[r].sort((a, b) => a.colIndex - b.colIndex)
    for (let i = 0; i + 3 < arr.length; i++) {
      if (arr[i + 1].colIndex === arr[i].colIndex + 1 && arr[i + 2].colIndex === arr[i].colIndex + 2 && arr[i + 3].colIndex === arr[i].colIndex + 3) {
        run = [arr[i], arr[i + 1], arr[i + 2], arr[i + 3]]; break
      }
    }
    if (run) break
  }
  ok('找到 4 个同排连续空位', !!run)
  const [a, b, c, d] = run

  // 1) 功能闭环：3 座组队成功
  const g1 = await api('/reservations/group', { method: 'POST', token: t.s1, body: {
    roomId, date, startTime: start, endTime: end,
    members: [{ seatId: a.seatId, username: 'student1' }, { seatId: b.seatId, username: 'student2' }, { seatId: c.seatId, username: 'student3' }]
  }})
  ok('组队预约成功（3 座）', g1.code === '0' && (g1.data || []).length === 3, JSON.stringify(g1).slice(0, 160))
  const r1 = (await api('/reservations/me', { token: t.s1 })).data
  const r2 = (await api('/reservations/me', { token: t.s2 })).data
  ok('成员各自名下均有预约', r1.some(r => r.seatId === a.seatId) && r2.some(r => r.seatId === b.seatId))

  // 2) 非相邻应拒绝（座位 a 与 d 之间隔了 b、c，但选 a、d 不连续）
  const nonAdj = await api('/reservations/group', { method: 'POST', token: t.s4, body: {
    roomId, date, startTime: start, endTime: end,
    members: [{ seatId: a.seatId, username: 'student4' }, { seatId: d.seatId, username: 'student5' }]
  }})
  ok('非相邻座位被拒绝', nonAdj.code !== '0', JSON.stringify(nonAdj).slice(0, 120))

  // 3) 并发原子性：换一个时段（成员在该时段均空闲），复用同一排 a、b、c
  const start2 = '13:00', end2 = '15:00'
  const [x, y, z] = [a, b, c]
  // 组 A 抢 {x,y}，组 B 抢 {y,z}，重叠在 y —— 期望恰好一组整体成功
  const teamA = api('/reservations/group', { method: 'POST', token: t.s1, body: { roomId, date, startTime: start2, endTime: end2, members: [{ seatId: x.seatId, username: 'student1' }, { seatId: y.seatId, username: 'student2' }] } })
  const teamB = api('/reservations/group', { method: 'POST', token: t.s3, body: { roomId, date, startTime: start2, endTime: end2, members: [{ seatId: y.seatId, username: 'student3' }, { seatId: z.seatId, username: 'student4' }] } })
  const [ra, rb] = await Promise.all([teamA, teamB])
  const successes = [ra, rb].filter(r => r.code === '0').length
  ok('并发两组恰好一组整体成功', successes === 1, `A=${ra.code} B=${rb.code}`)
  const after = (await api(`/study-rooms/${roomId}/board?date=${date}&start=${start2}&end=${end2}`, { token: t.s1 })).data.seats
  const st = id => after.find(s => s.seatId === id)?.status
  const winnerIsA = ra.code === '0'
  ok('胜方两座均 RESERVED', winnerIsA ? (st(x.seatId) === 'RESERVED' && st(y.seatId) === 'RESERVED') : (st(y.seatId) === 'RESERVED' && st(z.seatId) === 'RESERVED'), `x=${st(x.seatId)} y=${st(y.seatId)} z=${st(z.seatId)}`)
  ok('败方独占座未被误占（原子回滚）', winnerIsA ? st(z.seatId) === 'FREE' : st(x.seatId) === 'FREE', `x=${st(x.seatId)} z=${st(z.seatId)}`)

  console.log(`\n结果：${pass} 通过 / ${fail} 失败`)
  process.exit(fail ? 1 : 0)
})().catch(e => { console.error('测试异常', e); process.exit(1) })
