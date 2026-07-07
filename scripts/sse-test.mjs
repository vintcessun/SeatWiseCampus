// 验证 SSE 实时看板：订阅后触发预约，确认收到 seat_reserved 事件
const BASE = process.env.BASE || 'http://localhost:8888'

async function api(path, opt = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (opt.token) headers['satoken'] = opt.token
  const res = await fetch(BASE + path, { method: opt.method || 'GET', headers, body: opt.body ? JSON.stringify(opt.body) : undefined })
  return (await res.json().catch(() => ({})))
}
async function login(u, p) { return (await api('/api/auth/login', { method: 'POST', body: { username: u, password: p } })).data?.token }

function tomorrow() { const d = new Date(); d.setDate(d.getDate() + 1); return d.toISOString().slice(0, 10) }

async function main() {
  const date = tomorrow()
  const roomId = 1
  const token = await login('student3', '123456')

  // 找一个空闲座位（用较晚时段，避免与其它测试冲突）
  const board = (await api(`/api/study-rooms/${roomId}/board?date=${date}&start=20:00&end=21:00`, { token })).data
  const seat = board.seats.find(s => s.status === 'FREE')
  console.log(`目标座位 ${seat.seatNo} (id=${seat.seatId})`)

  // 建立 SSE 连接（token 走查询参数）
  const events = []
  const ctrl = new AbortController()
  const streamUrl = `${BASE}/api/board/stream?roomId=${roomId}&date=${date}&token=${token}`
  const resp = await fetch(streamUrl, { headers: { Accept: 'text/event-stream' }, signal: ctrl.signal })
  const reader = resp.body.getReader()
  const dec = new TextDecoder()
  ;(async () => {
    let buf = ''
    try {
      while (true) {
        const { value, done } = await reader.read()
        if (done) break
        buf += dec.decode(value, { stream: true })
        let idx
        while ((idx = buf.indexOf('\n')) >= 0) {
          const line = buf.slice(0, idx).trim(); buf = buf.slice(idx + 1)
          if (line.startsWith('event:')) events.push(line.slice(6).trim())
        }
      }
    } catch (e) { /* aborted */ }
  })()

  await new Promise(r => setTimeout(r, 800))
  console.log('已订阅 SSE，触发预约…')
  const r = await api('/api/reservations', { method: 'POST', token, body: { roomId, seatId: seat.seatId, date, startTime: '20:00', endTime: '21:00' } })
  console.log('预约结果:', r.code)

  await new Promise(r => setTimeout(r, 1500))
  ctrl.abort()

  const got = events.filter(e => e !== 'heartbeat')
  console.log('收到事件:', got.length ? got.join(', ') : '(无)')
  const ok = got.includes('seat_reserved')
  console.log(ok ? '\n✅ SSE 实时推送验证通过（收到 seat_reserved）' : '\n❌ 未收到 seat_reserved 事件')
  process.exit(ok ? 0 : 1)
}
main().catch(e => { console.error(e); process.exit(1) })
