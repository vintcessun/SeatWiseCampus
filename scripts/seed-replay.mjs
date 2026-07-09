// 为「历史回放」演示生成一批当天预约（未来时段），形成有起伏的占用曲线
const BASE = 'http://localhost:8888/api'
async function api(path, { method = 'GET', token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['satoken'] = token
  const res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  return res.json().catch(() => ({}))
}
const login = async (u, p) => (await api('/auth/login', { method: 'POST', body: { username: u, password: p } })).data
const today = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` }
const pad = x => String(Math.floor(x / 60) % 24).padStart(2, '0') + ':' + String(x % 60).padStart(2, '0')

;(async () => {
  const date = today()
  const toks = {}
  for (let i = 1; i <= 8; i++) toks[i] = (await login('student' + i, '123456')).token

  const roomId = (await api(`/study-rooms?campusId=${(await api('/campuses', { token: toks[1] })).data[0].id}`, { token: toks[1] })).data[0].id
  const seats = (await api(`/study-rooms/${roomId}/board?date=${date}`, { token: toks[1] })).data.seats
    .filter(s => s.cellType === 'SEAT' && s.status === 'FREE').map(s => s.seatId)

  // 从下一个整点后的时段开始（保证 start > now）
  const now = new Date(); let nowSlotMin = now.getHours() * 60 + now.getMinutes()
  let s0 = Math.ceil(nowSlotMin / 30) * 30 + 30  // 留出余量
  const windows = []
  for (let s = s0; s + 60 <= 22 * 60 && windows.length < 6; s += 60) windows.push(s)
  if (!windows.length) { console.log('当前已接近闭馆，改用最后 3 个时段'); for (let s = 19 * 60; s < 22 * 60; s += 60) windows.push(s) }

  const targets = [3, 5, 6, 5, 3, 2]
  const usage = {}; for (let i = 1; i <= 8; i++) usage[i] = 0
  let created = 0, failed = 0, si = 0
  for (let w = 0; w < windows.length; w++) {
    const s = windows[w], startTime = pad(s), endTime = pad(s + 60)
    const need = Math.min(targets[w] ?? 3, 8)
    let picked = 0, tries = 0
    for (let uid = 1; uid <= 8 && picked < need && tries < 20; ) {
      if (usage[uid] < 3) {
        const seatId = seats[si % seats.length]; si++
        const r = await api('/reservations', { method: 'POST', token: toks[uid], body: { roomId, seatId, date, startTime, endTime } })
        if (r.code === '0') { usage[uid]++; picked++; created++ } else { failed++ }
      }
      uid++
      if (uid > 8) { uid = 1; tries++; if (picked >= need) break }
    }
    console.log(`  ${startTime}-${endTime}: 目标 ${need}, 实际 ${picked}`)
  }
  console.log(`\n生成完成：${created} 条预约（失败 ${failed}），房间 ${roomId}，日期 ${date}`)
})().catch(e => { console.error(e); process.exit(1) })
