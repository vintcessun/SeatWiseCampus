// 历史回放测试：预约后应在对应时间片的回放帧中出现，峰值/利用率可计算
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
const today = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` }
const pad = x => String(Math.floor(x / 60) % 24).padStart(2, '0') + ':' + String(x % 60).padStart(2, '0')

;(async () => {
  console.log('历史回放测试')
  const date = today()
  // 注册一个全新学生，避免受既有预约/每日上限污染
  const uname = 'replaytest_' + Date.now().toString().slice(-8)
  const reg = await api('/auth/register', { method: 'POST', body: { username: uname, password: '123456', realName: '回放测试' } })
  const stu = reg.data?.token || await login(uname, '123456')
  const admin = await login('admin', 'admin123')
  const roomId = (await api(`/study-rooms?campusId=${(await api('/campuses', { token: stu })).data[0].id}`, { token: stu })).data[0].id

  // 选一个未来时段（下一个整点之后 1 小时窗口）
  const now = new Date(); const s = Math.ceil((now.getHours() * 60 + now.getMinutes()) / 30) * 30 + 30
  if (s + 60 > 22 * 60) { console.log('  临近闭馆，跳过'); process.exit(0) }
  const startTime = pad(s), endTime = pad(s + 60)
  const seat = (await api(`/study-rooms/${roomId}/board?date=${date}&start=${startTime}&end=${endTime}`, { token: stu })).data.seats.find(x => x.status === 'FREE')

  const before = (await api(`/study-rooms/${roomId}/replay?date=${date}`, { token: admin })).data
  ok('回放返回时间片轴', Array.isArray(before.timeline) && before.timeline.length > 0, `len=${before.timeline?.length}`)
  const frameBefore = before.timeline.find(f => f.slotIndex === s / 30)
  const occBefore = frameBefore ? frameBefore.occupiedCount : 0

  const cr = await api('/reservations', { method: 'POST', token: stu, body: { roomId, seatId: seat.seatId, date, startTime, endTime } })
  ok('预约成功', cr.code === '0', JSON.stringify(cr).slice(0, 120))

  const after = (await api(`/study-rooms/${roomId}/replay?date=${date}`, { token: admin })).data
  const frameAfter = after.timeline.find(f => f.slotIndex === s / 30)
  ok('该时段回放帧占用 +1', frameAfter && frameAfter.occupiedCount === occBefore + 1, `before=${occBefore} after=${frameAfter?.occupiedCount}`)
  ok('该座位出现在回放帧占用列表', frameAfter && frameAfter.occupied.includes(seat.seatId))
  ok('回放帧带时间标签', frameAfter && /^\d{2}:\d{2}$/.test(frameAfter.label), frameAfter?.label)
  ok('totalSeats 合理(>0)', after.totalSeats > 0, `${after.totalSeats}`)
  // 早于开馆或结束后的时段不含该预约
  const otherFrame = after.timeline.find(f => f.slotIndex === (s / 30) - 1)
  ok('相邻更早时段不含该预约', !otherFrame || !otherFrame.occupied.includes(seat.seatId))

  console.log(`\n结果：${pass} 通过 / ${fail} 失败`)
  process.exit(fail ? 1 : 0)
})().catch(e => { console.error('测试异常', e); process.exit(1) })
