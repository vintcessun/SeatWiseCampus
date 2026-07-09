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
class CDP { constructor(ws){this.ws=ws;this.id=0;this.p=new Map()} static async attach(port){for(let i=0;i<30;i++){try{const l=await(await fetch(`http://127.0.0.1:${port}/json`)).json();const pg=l.find(t=>t.type==='page'&&t.webSocketDebuggerUrl);if(pg){const ws=new WebSocket(pg.webSocketDebuggerUrl);await new Promise((res,rej)=>{ws.addEventListener('open',res,{once:true});ws.addEventListener('error',rej,{once:true})});const c=new CDP(ws);ws.addEventListener('message',e=>{const m=JSON.parse(e.data);if(m.id&&c.p.has(m.id)){c.p.get(m.id)(m);c.p.delete(m.id)}});return c}}catch{}await sleep(500)}throw new Error('no cdp')} send(method,params={}){const id=++this.id;return new Promise(r=>{this.p.set(id,m=>r(m.result));this.ws.send(JSON.stringify({id,method,params}))})} }

async function main() {
  const stu = await login('student1', '123456')
  const dark = process.argv.includes('--dark')
  const userDir = join(os.tmpdir(), 'sw-st-' + Date.now())
  const chrome = spawn(CHROME, ['--headless=new', '--disable-gpu', '--no-sandbox', '--no-first-run', '--hide-scrollbars', '--remote-debugging-port=9245', '--user-data-dir=' + userDir, '--window-size=1440,940', '--lang=zh-CN', 'about:blank'], { stdio: 'ignore' })
  await sleep(2500)
  const cdp = await CDP.attach(9245)
  await cdp.send('Page.enable'); await cdp.send('Runtime.enable')
  const ev = e => cdp.send('Runtime.evaluate', { expression: e, returnByValue: true })
  const goto = async (u, w = 3200) => { await cdp.send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 940, deviceScaleFactor: 2, mobile: false }); await cdp.send('Page.navigate', { url: u }); await sleep(w) }
  await goto(WEB + '/login', 1000)
  await ev(`localStorage.setItem('sw-theme','${dark ? 'dark' : 'light'}');localStorage.setItem('satoken','${stu.token}');localStorage.setItem('role','${stu.role}');localStorage.setItem('userInfo',JSON.stringify(${JSON.stringify(stu.userInfo)}))`)
  await goto(WEB + '/student/spacetime', 3600)
  // 拖到上午 10:00 附近（第 5 根柱）以展示占用与热力对比
  await ev(`(()=>{const bars=document.querySelectorAll('.spark .bar');if(bars[5])bars[5].click()})()`)
  await sleep(1000)
  const res = await cdp.send('Page.captureScreenshot', { format: 'png', fromSurface: true })
  const file = dark ? '30-spacetime-dark.png' : '29-spacetime.png'
  writeFileSync(join(__dirname, 'shots', file), Buffer.from(res.data, 'base64'))
  console.log('📸 ' + file)
  chrome.kill(); process.exit(0)
}
main().catch(e => { console.error(e); process.exit(1) })
