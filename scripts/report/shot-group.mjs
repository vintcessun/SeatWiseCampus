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
  const A = await login('student1', '123456')
  const roomId = (await api(`/api/study-rooms?campusId=${(await api('/api/campuses', { token: A.token })).data[0].id}`, { token: A.token })).data[0].id

  const userDir = join(os.tmpdir(), 'sw-group-' + Date.now())
  const chrome = spawn(CHROME, ['--headless=new', '--disable-gpu', '--no-sandbox', '--no-first-run', '--hide-scrollbars', '--remote-debugging-port=9232', '--user-data-dir=' + userDir, '--window-size=1440,1080', '--lang=zh-CN', 'about:blank'], { stdio: 'ignore' })
  await sleep(2500)
  const cdp = await CDP.attach(9232)
  await cdp.send('Page.enable'); await cdp.send('Runtime.enable')
  const ev = e => cdp.send('Runtime.evaluate', { expression: e, returnByValue: true })
  async function goto(u, w = 2200) { await cdp.send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1080, deviceScaleFactor: 2, mobile: false }); await cdp.send('Page.navigate', { url: u }); await sleep(w) }
  await goto(WEB + '/login', 1200)
  await ev(`localStorage.setItem('satoken','${A.token}');localStorage.setItem('role','${A.role}');localStorage.setItem('userInfo',JSON.stringify(${JSON.stringify(A.userInfo)}))`)
  await goto(WEB + `/student/rooms/${roomId}/seats`, 2600)
  // 打开「组队相邻预约」开关
  await ev(`document.querySelector('.el-switch').click()`)
  await sleep(600)
  // 选 3 个连续空位（DOM 顺序按行列，取前 3 个 clickable）
  await ev(`Array.from(document.querySelectorAll('.seat-cell.clickable')).slice(0,3).forEach(el=>el.click())`)
  await sleep(700)
  // 为成员填入用户名
  await ev(`(()=>{const set=(el,v)=>{const s=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;s.call(el,v);el.dispatchEvent(new Event('input',{bubbles:true}))};const ins=document.querySelectorAll('input[placeholder=\\'成员用户名\\']');['student1','student2','student3'].forEach((v,i)=>{if(ins[i])set(ins[i],v)})})()`)
  await sleep(800)
  const res = await cdp.send('Page.captureScreenshot', { format: 'png', fromSurface: true })
  writeFileSync(join(__dirname, 'shots', '20-group.png'), Buffer.from(res.data, 'base64'))
  console.log('📸 20-group.png')
  chrome.kill(); process.exit(0)
}
main().catch(e => { console.error(e); process.exit(1) })
