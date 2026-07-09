import { spawn } from 'child_process'
import { writeFileSync } from 'fs'
import { fileURLToPath } from 'url'
import { dirname, join } from 'path'
import os from 'os'
const __dirname = dirname(fileURLToPath(import.meta.url))
const WEB = 'http://localhost:8888'
const CHROME = 'C:/Users/vintces/.cache/puppeteer/chrome/win64-140.0.7339.82/chrome-win64/chrome.exe'
const sleep = ms => new Promise(r => setTimeout(r, ms))
async function api(path, opt = {}) { const h = { 'Content-Type': 'application/json' }; if (opt.token) h['satoken'] = opt.token; const r = await fetch(WEB + path, { method: opt.method || 'GET', headers: h, body: opt.body ? JSON.stringify(opt.body) : undefined }); return await r.json().catch(() => ({})) }
const login = async (u, p) => (await api('/api/auth/login', { method: 'POST', body: { username: u, password: p } })).data
function today() { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` }

class CDP { constructor(ws) { this.ws = ws; this.id = 0; this.p = new Map() }
  static async attach(port) { for (let i = 0; i < 30; i++) { try { const l = await (await fetch(`http://127.0.0.1:${port}/json`)).json(); const pg = l.find(t => t.type === 'page' && t.webSocketDebuggerUrl); if (pg) { const ws = new WebSocket(pg.webSocketDebuggerUrl); await new Promise((res, rej) => { ws.addEventListener('open', res, { once: true }); ws.addEventListener('error', rej, { once: true }) }); const c = new CDP(ws); ws.addEventListener('message', e => { const m = JSON.parse(e.data); if (m.id && c.p.has(m.id)) { c.p.get(m.id)(m); c.p.delete(m.id) } }); return c } } catch {} await sleep(500) } throw new Error('no cdp') }
  send(method, params = {}) { const id = ++this.id; return new Promise(r => { this.p.set(id, m => r(m.result)); this.ws.send(JSON.stringify({ id, method, params })) }) } }

async function main() {
  const T = today()
  const stu = await login('student1', '123456')
  const s2 = (await login('student2', '123456')).token
  // student2 锁两个座位（今天 14:00-16:00），让 student1 看到"选择中"倒计时
  const board = (await api(`/api/study-rooms/1/board?date=${T}&start=14:00&end=16:00`, { token: stu.token })).data
  const free = board.seats.filter(s => s.status === 'FREE').slice(0, 2)
  for (const s of free) await api('/api/holds', { method: 'POST', token: s2, body: { roomId: 1, seatId: s.seatId, date: T, startTime: '14:00', endTime: '16:00' } })

  const userDir = join(os.tmpdir(), 'sw-hold-' + Date.now())
  const chrome = spawn(CHROME, ['--headless=new', '--disable-gpu', '--no-sandbox', '--no-first-run', '--hide-scrollbars', '--remote-debugging-port=9224', '--user-data-dir=' + userDir, '--window-size=1440,1000', '--lang=zh-CN', 'about:blank'], { stdio: 'ignore' })
  await sleep(2500)
  const cdp = await CDP.attach(9224)
  await cdp.send('Page.enable'); await cdp.send('Runtime.enable')
  const ev = e => cdp.send('Runtime.evaluate', { expression: e, returnByValue: true })
  async function goto(u, w = 2200) { await cdp.send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 2, mobile: false }); await cdp.send('Page.navigate', { url: u }); await sleep(w) }
  await goto(WEB + '/login', 1200)
  await ev(`localStorage.setItem('satoken','${stu.token}');localStorage.setItem('role','${stu.role}');localStorage.setItem('userInfo',JSON.stringify(${JSON.stringify(stu.userInfo)}))`)
  await goto(WEB + '/student/rooms/1/seats', 2800)
  const m = await cdp.send('Page.getLayoutMetrics'); const cs = m.cssContentSize || m.contentSize
  await cdp.send('Emulation.setDeviceMetricsOverride', { width: Math.ceil(cs.width), height: Math.min(Math.ceil(cs.height), 1200), deviceScaleFactor: 2, mobile: false }); await sleep(300)
  const res = await cdp.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true, fromSurface: true })
  writeFileSync(join(__dirname, 'shots', '14-seat-hold.png'), Buffer.from(res.data, 'base64'))
  console.log('📸 14-seat-hold.png')
  chrome.kill(); process.exit(0)
}
main().catch(e => { console.error(e); process.exit(1) })
