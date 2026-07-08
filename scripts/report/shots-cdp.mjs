// 无依赖截图：直接用 CDP 驱动缓存的 Chrome（Node 内置 WebSocket/fetch），输出到 shots/
import { spawn } from 'child_process'
import { mkdirSync, writeFileSync, rmSync } from 'fs'
import { fileURLToPath } from 'url'
import { dirname, join } from 'path'
import os from 'os'

const __dirname = dirname(fileURLToPath(import.meta.url))
const OUT = join(__dirname, 'shots')
mkdirSync(OUT, { recursive: true })
const WEB = process.env.WEB || 'http://localhost:8888'
const DOC = process.env.DOC || 'http://localhost:18080'
const CHROME = process.env.CHROME_PATH ||
  'C:/Users/vintces/.cache/puppeteer/chrome/win64-140.0.7339.82/chrome-win64/chrome.exe'
const sleep = (ms) => new Promise(r => setTimeout(r, ms))

async function apiLogin(u, p) {
  const r = await fetch(WEB + '/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: u, password: p }) })
  return (await r.json()).data
}

// ---- 极简 CDP 客户端 ----
class CDP {
  constructor(ws) { this.ws = ws; this.id = 0; this.pending = new Map() }
  static async attach() {
    // 找 page 目标
    for (let i = 0; i < 30; i++) {
      try {
        const list = await (await fetch('http://127.0.0.1:9222/json')).json()
        const page = list.find(t => t.type === 'page' && t.webSocketDebuggerUrl)
        if (page) {
          const ws = new WebSocket(page.webSocketDebuggerUrl)
          await new Promise((res, rej) => { ws.addEventListener('open', res, { once: true }); ws.addEventListener('error', rej, { once: true }) })
          const cdp = new CDP(ws)
          ws.addEventListener('message', (e) => {
            const msg = JSON.parse(e.data)
            if (msg.id && cdp.pending.has(msg.id)) { cdp.pending.get(msg.id)(msg); cdp.pending.delete(msg.id) }
          })
          return cdp
        }
      } catch {}
      await sleep(500)
    }
    throw new Error('无法连接到 Chrome CDP')
  }
  send(method, params = {}) {
    const id = ++this.id
    return new Promise((resolve) => {
      this.pending.set(id, (msg) => resolve(msg.result))
      this.ws.send(JSON.stringify({ id, method, params }))
    })
  }
}

async function main() {
  const stu = await apiLogin('student1', '123456')
  const adm = await apiLogin('admin', 'admin123')

  const userDir = join(os.tmpdir(), 'sw-chrome-' + Date.now())
  const chrome = spawn(CHROME, [
    '--headless=new', '--disable-gpu', '--no-sandbox', '--no-first-run', '--no-default-browser-check',
    '--hide-scrollbars', '--remote-debugging-port=9222', '--user-data-dir=' + userDir,
    '--window-size=1440,1000', '--lang=zh-CN', 'about:blank'
  ], { stdio: 'ignore' })

  await sleep(2500)
  const cdp = await CDP.attach()
  await cdp.send('Page.enable')
  await cdp.send('Runtime.enable')

  async function goto(url, wait = 2200) {
    await cdp.send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 2, mobile: false })
    await cdp.send('Page.navigate', { url })
    await sleep(wait)
  }
  async function evaluate(expr) {
    return cdp.send('Runtime.evaluate', { expression: expr, returnByValue: true })
  }
  async function setAuth(a) {
    await evaluate(`localStorage.setItem('satoken','${a.token}');localStorage.setItem('role','${a.role}');localStorage.setItem('userInfo',JSON.stringify(${JSON.stringify(a.userInfo)}))`)
  }
  async function shot(name, extraWait = 0) {
    if (extraWait) await sleep(extraWait)
    const m = await cdp.send('Page.getLayoutMetrics')
    const cs = m.cssContentSize || m.contentSize
    const w = Math.ceil(cs.width), h = Math.min(Math.ceil(cs.height), 4000)
    await cdp.send('Emulation.setDeviceMetricsOverride', { width: w, height: h, deviceScaleFactor: 2, mobile: false })
    await sleep(300)
    const res = await cdp.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true, fromSurface: true })
    writeFileSync(join(OUT, name), Buffer.from(res.data, 'base64'))
    console.log('  📸', name)
  }

  // 建立 origin 并注入学生登录态
  await goto(WEB + '/login', 1500)
  await evaluate('localStorage.clear()')
  await goto(WEB + '/login', 1500)
  await shot('01-login.png')

  await setAuth(stu)
  await goto(WEB + '/student/rooms'); await shot('02-student-rooms.png')
  await goto(WEB + '/student/rooms/1/seats', 2600); await shot('03-student-seats.png')
  await goto(WEB + '/student/reservations'); await shot('04-student-reservations.png')
  await goto(WEB + '/student/nearby', 1800)
  await evaluate(`[...document.querySelectorAll('button')].find(b=>b.textContent.includes('推荐'))?.click()`)
  await shot('05-student-nearby.png', 1800)
  await goto(WEB + '/student/ranking'); await shot('06-student-ranking.png')

  await setAuth(adm)
  await goto(WEB + '/admin/rooms'); await shot('07-admin-rooms.png')
  await goto(WEB + '/admin/rooms/1/layout', 2400); await shot('08-admin-layout.png')
  await goto(WEB + '/admin/rooms/1/board', 2600); await shot('09-admin-board.png')
  await goto(WEB + '/admin/reports', 2000); await shot('10-admin-reports.png', 2500)
  await goto(WEB + '/admin/blacklist'); await shot('11-admin-blacklist.png')

  await goto(DOC + '/doc.html', 2000); await shot('12-knife4j.png', 2200)

  chrome.kill()
  try { rmSync(userDir, { recursive: true, force: true }) } catch {}
  console.log('全部截图完成 ->', OUT)
  process.exit(0)
}
main().catch(e => { console.error(e); process.exit(1) })
