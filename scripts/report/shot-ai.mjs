// 截取 AI 助手工作截图（无依赖 CDP）
import { spawn } from 'child_process'
import { writeFileSync } from 'fs'
import { fileURLToPath } from 'url'
import { dirname, join } from 'path'
import os from 'os'

const __dirname = dirname(fileURLToPath(import.meta.url))
const WEB = 'http://localhost:8888'
const CHROME = 'C:/Users/vintces/.cache/puppeteer/chrome/win64-140.0.7339.82/chrome-win64/chrome.exe'
const sleep = (ms) => new Promise(r => setTimeout(r, ms))

async function apiLogin(u, p) {
  const r = await fetch(WEB + '/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: u, password: p }) })
  return (await r.json()).data
}

class CDP {
  constructor(ws) { this.ws = ws; this.id = 0; this.pending = new Map() }
  static async attach() {
    for (let i = 0; i < 30; i++) {
      try {
        const list = await (await fetch('http://127.0.0.1:9223/json')).json()
        const page = list.find(t => t.type === 'page' && t.webSocketDebuggerUrl)
        if (page) {
          const ws = new WebSocket(page.webSocketDebuggerUrl)
          await new Promise((res, rej) => { ws.addEventListener('open', res, { once: true }); ws.addEventListener('error', rej, { once: true }) })
          const cdp = new CDP(ws)
          ws.addEventListener('message', (e) => { const m = JSON.parse(e.data); if (m.id && cdp.pending.has(m.id)) { cdp.pending.get(m.id)(m); cdp.pending.delete(m.id) } })
          return cdp
        }
      } catch {}
      await sleep(500)
    }
    throw new Error('no cdp')
  }
  send(method, params = {}) { const id = ++this.id; return new Promise(r => { this.pending.set(id, m => r(m.result)); this.ws.send(JSON.stringify({ id, method, params })) }) }
}

async function main() {
  const stu = await apiLogin('student1', '123456')
  const userDir = join(os.tmpdir(), 'sw-ai-' + Date.now())
  const chrome = spawn(CHROME, ['--headless=new', '--disable-gpu', '--no-sandbox', '--no-first-run', '--hide-scrollbars',
    '--remote-debugging-port=9223', '--user-data-dir=' + userDir, '--window-size=1440,1000', '--lang=zh-CN', 'about:blank'], { stdio: 'ignore' })
  await sleep(2500)
  const cdp = await CDP.attach()
  await cdp.send('Page.enable'); await cdp.send('Runtime.enable')

  async function goto(url, wait = 2200) {
    await cdp.send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 2, mobile: false })
    await cdp.send('Page.navigate', { url }); await sleep(wait)
  }
  const evaluate = (expr) => cdp.send('Runtime.evaluate', { expression: expr, returnByValue: true })

  await goto(WEB + '/login', 1500)
  await evaluate(`localStorage.setItem('satoken','${stu.token}');localStorage.setItem('role','${stu.role}');localStorage.setItem('userInfo',JSON.stringify(${JSON.stringify(stu.userInfo)}))`)
  await goto(WEB + '/student/rooms', 2200)
  // 打开 AI 面板
  await evaluate(`document.querySelector('.ai-fab').click()`)
  await sleep(600)
  // 点第一个示例，触发推荐
  await evaluate(`document.querySelectorAll('.ai-chip')[0].click()`)
  await sleep(2200)

  const m = await cdp.send('Page.getLayoutMetrics')
  const cs = m.cssContentSize || m.contentSize
  await cdp.send('Emulation.setDeviceMetricsOverride', { width: Math.ceil(cs.width), height: Math.min(Math.ceil(cs.height), 1200), deviceScaleFactor: 2, mobile: false })
  await sleep(300)
  const res = await cdp.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true, fromSurface: true })
  writeFileSync(join(__dirname, 'shots', '13-ai-assistant.png'), Buffer.from(res.data, 'base64'))
  console.log('📸 13-ai-assistant.png')
  chrome.kill()
  process.exit(0)
}
main().catch(e => { console.error(e); process.exit(1) })
