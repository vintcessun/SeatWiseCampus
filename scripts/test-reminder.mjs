// 预约提醒测试：预约后由定时任务推送「即将开始」提醒（30 分钟内），且幂等只推一次
const BASE = 'http://localhost:8888/api'
let pass = 0, fail = 0
function ok(name, cond, extra = '') { if (cond) { pass++; console.log(`  ✓ ${name}`) } else { fail++; console.log(`  ✗ ${name} ${extra}`) } }
async function api(path, { method = 'GET', token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['satoken'] = token
  const res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  return res.json().catch(() => ({}))
}
const today = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` }
const pad = x => String(Math.floor(x / 60) % 24).padStart(2, '0') + ':' + String(x % 60).padStart(2, '0')
const sleep = ms => new Promise(r => setTimeout(r, ms))

;(async () => {
  console.log('预约提醒测试')
  const date = today()
  // 全新用户，避免历史提醒干扰
  const uname = 'remindtest_' + Date.now().toString().slice(-8)
  const cap = (await api('/captcha')).data
  const reg = await api('/auth/register', { method: 'POST', body: { username: uname, password: '123456', realName: '提醒测试', captchaId: cap.captchaId, captchaCode: cap.code } })
  const token = reg.data?.token
  ok('注册测试用户', !!token)

  // 下一个 30 分钟边界（严格晚于当前时间），确保在 remindBefore(30) 窗口内
  const now = new Date(); const cur = now.getHours() * 60 + now.getMinutes()
  let s = (Math.floor(cur / 30) + 1) * 30
  if (s + 60 > 22 * 60) { console.log('  临近闭馆，跳过'); process.exit(0) }
  const startTime = pad(s), endTime = pad(s + 60)

  const roomId = (await api('/study-rooms?campusId=1', { token })).data[0].id
  const seat = (await api(`/study-rooms/${roomId}/board?date=${date}&start=${startTime}&end=${endTime}`, { token })).data.seats.find(x => x.status === 'FREE')
  const cr = await api('/reservations', { method: 'POST', token, body: { roomId, seatId: seat.seatId, date, startTime, endTime } })
  ok('预约成功(下一个时段)', cr.code === '0', JSON.stringify(cr).slice(0, 120))

  const before = ((await api('/notifications', { token })).data || []).filter(n => n.type === 'REMINDER').length

  // 定时任务每 5s 扫描一次，等待两个周期
  await sleep(7000)
  let notis = (await api('/notifications', { token })).data || []
  let rem = notis.filter(n => n.type === 'REMINDER')
  ok('收到「即将开始」提醒(REMINDER)', rem.length === before + 1, `before=${before} after=${rem.length}`)
  ok('提醒文案含即将开始', rem.some(n => (n.title || '').includes('即将开始')), JSON.stringify(rem.map(n => n.title)))

  // 幂等：再等一个周期，数量不应增加
  await sleep(6000)
  notis = (await api('/notifications', { token })).data || []
  const rem2 = notis.filter(n => n.type === 'REMINDER').length
  ok('提醒幂等（不重复推送）', rem2 === rem.length, `first=${rem.length} second=${rem2}`)

  console.log(`\n结果：${pass} 通过 / ${fail} 失败`)
  process.exit(fail ? 1 : 0)
})().catch(e => { console.error('测试异常', e); process.exit(1) })
