// 为截图/演示造数据：明天在 A301 的多状态预约 + 一条黑名单
const BASE = process.env.BASE || 'http://localhost:8888'
const ROOM = 1

async function api(path, opt = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (opt.token) headers['satoken'] = opt.token
  const res = await fetch(BASE + path, { method: opt.method || 'GET', headers, body: opt.body ? JSON.stringify(opt.body) : undefined })
  return await res.json().catch(() => ({}))
}
const login = async (u, p) => (await api('/api/auth/login', { method: 'POST', body: { username: u, password: p } })).data?.token
function tomorrow() { const d = new Date(); d.setDate(d.getDate() + 1); return d.toISOString().slice(0, 10) }

async function freeSeats(token, date, start, end, n) {
  const b = (await api(`/api/study-rooms/${ROOM}/board?date=${date}&start=${start}&end=${end}`, { token })).data
  return b.seats.filter(s => s.status === 'FREE').slice(0, n).map(s => s.seatId)
}
async function reserve(token, seatId, date, start, end) {
  return api('/api/reservations', { method: 'POST', token, body: { roomId: ROOM, seatId, date, startTime: start, endTime: end } })
}

async function main() {
  const date = tomorrow()
  const s1 = await login('student1', '123456')
  const s2 = await login('student2', '123456')
  const s3 = await login('student3', '123456')
  const s5 = await login('student5', '123456')
  const s6 = await login('student6', '123456')
  const s7 = await login('student7', '123456')

  // 14:00-16:00 三个座位：student1(待签到,mine) / student2(待签到) / student3(使用中)
  const seats1416 = await freeSeats(s1, date, '14:00', '16:00', 5)
  await reserve(s1, seats1416[0], date, '14:00', '16:00')
  await reserve(s2, seats1416[1], date, '14:00', '16:00')
  const r3 = await reserve(s3, seats1416[2], date, '14:00', '16:00')
  if (r3.data?.id) await api(`/api/reservations/${r3.data.id}/check-in`, { method: 'POST', token: s3 })

  // 已完成：student5 10:00-11:00 签到+签退
  const seat10 = (await freeSeats(s5, date, '10:00', '11:00', 1))[0]
  const r5 = await reserve(s5, seat10, date, '10:00', '11:00')
  if (r5.data?.id) {
    await api(`/api/reservations/${r5.data.id}/check-in`, { method: 'POST', token: s5 })
    await api(`/api/reservations/${r5.data.id}/check-out`, { method: 'POST', token: s5 })
  }

  // 已取消：student6 18:00-19:00
  const seat18 = (await freeSeats(s6, date, '18:00', '19:00', 1))[0]
  const r6 = await reserve(s6, seat18, date, '18:00', '19:00')
  if (r6.data?.id) await api(`/api/reservations/${r6.data.id}/cancel`, { method: 'POST', token: s6 })

  // 热门时段丰富：student7 16:00-18:00
  const seat16 = (await freeSeats(s7, date, '16:00', '18:00', 1))[0]
  await reserve(s7, seat16, date, '16:00', '18:00')

  console.log('演示数据创建完成，日期 =', date)
}
main().catch(e => { console.error(e); process.exit(1) })
