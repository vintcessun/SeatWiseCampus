// 用 puppeteer 截取各页面截图，输出到 scripts/report/shots/
import puppeteer from 'puppeteer'
import { mkdirSync } from 'fs'
import { fileURLToPath } from 'url'
import { dirname, join } from 'path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const OUT = join(__dirname, 'shots')
mkdirSync(OUT, { recursive: true })

const WEB = process.env.WEB || 'http://localhost:8888'
const DOC = process.env.DOC || 'http://localhost:18080'
const sleep = (ms) => new Promise(r => setTimeout(r, ms))
function tomorrow() { const d = new Date(); d.setDate(d.getDate() + 1); return d.toISOString().slice(0, 10) }

async function apiLogin(u, p) {
  const res = await fetch(WEB + '/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: u, password: p }) })
  return (await res.json()).data
}

async function main() {
  const stu = await apiLogin('student1', '123456')
  const adm = await apiLogin('admin', 'admin123')
  const T = tomorrow()

  const CHROME = process.env.CHROME_PATH ||
    'C:/Users/vintces/.cache/puppeteer/chrome/win64-140.0.7339.82/chrome-win64/chrome.exe'
  const browser = await puppeteer.launch({ headless: 'new', executablePath: CHROME, args: ['--no-sandbox', '--lang=zh-CN'] })
  const page = await browser.newPage()
  await page.setViewport({ width: 1440, height: 900, deviceScaleFactor: 2 })

  async function auth(a) {
    await page.goto(WEB + '/login', { waitUntil: 'networkidle2' })
    await page.evaluate((auth) => {
      localStorage.setItem('satoken', auth.token)
      localStorage.setItem('role', auth.role)
      localStorage.setItem('userInfo', JSON.stringify(auth.userInfo))
    }, a)
  }
  async function shot(name, sel) {
    if (sel) { try { await page.waitForSelector(sel, { timeout: 8000 }) } catch {} }
    await sleep(1200)
    await page.screenshot({ path: join(OUT, name), fullPage: true })
    console.log('  📸', name)
  }
  async function setDate(value) {
    try {
      const input = await page.$('.el-date-editor input')
      if (!input) return
      await input.click({ clickCount: 3 })
      await page.keyboard.type(value)
      await page.keyboard.press('Enter')
      await page.keyboard.press('Escape')
      await sleep(1000)
    } catch (e) { console.log('  setDate 失败', e.message) }
  }

  // 1. 登录页（清空登录态）
  await page.goto(WEB + '/login', { waitUntil: 'networkidle2' })
  await page.evaluate(() => localStorage.clear())
  await page.goto(WEB + '/login', { waitUntil: 'networkidle2' })
  await shot('01-login.png', '.login-card')

  // 学生端
  await auth(stu)
  await page.goto(WEB + '/student/rooms', { waitUntil: 'networkidle2' })
  await shot('02-student-rooms.png', '.el-card')

  await auth(stu)
  await page.goto(WEB + `/student/rooms/1/seats`, { waitUntil: 'networkidle2' })
  await setDate(T)
  await shot('03-student-seats.png', '.seat-grid')

  await auth(stu)
  await page.goto(WEB + '/student/reservations', { waitUntil: 'networkidle2' })
  await shot('04-student-reservations.png', '.el-table')

  await auth(stu)
  await page.goto(WEB + '/student/nearby', { waitUntil: 'networkidle2' })
  await setDate(T)
  try {
    const btns = await page.$$('button')
    for (const b of btns) { const t = await page.evaluate(el => el.textContent, b); if (t && t.includes('推荐')) { await b.click(); break } }
  } catch {}
  await shot('05-student-nearby.png', '.el-card')

  await auth(stu)
  await page.goto(WEB + '/student/ranking', { waitUntil: 'networkidle2' })
  await shot('06-student-ranking.png', '.el-table')

  // 管理端
  await auth(adm)
  await page.goto(WEB + '/admin/rooms', { waitUntil: 'networkidle2' })
  await shot('07-admin-rooms.png', '.el-table')

  await auth(adm)
  await page.goto(WEB + '/admin/rooms/1/layout', { waitUntil: 'networkidle2' })
  await shot('08-admin-layout.png', '.seat-grid')

  await auth(adm)
  await page.goto(WEB + '/admin/rooms/1/board', { waitUntil: 'networkidle2' })
  await setDate(T)
  await shot('09-admin-board.png', '.seat-grid')

  await auth(adm)
  await page.goto(WEB + '/admin/reports', { waitUntil: 'networkidle2' })
  await sleep(2500) // 等 ECharts 渲染
  await shot('10-admin-reports.png', 'canvas')

  await auth(adm)
  await page.goto(WEB + '/admin/blacklist', { waitUntil: 'networkidle2' })
  await shot('11-admin-blacklist.png', '.el-table')

  // Knife4j 接口文档
  try {
    await page.goto(DOC + '/doc.html', { waitUntil: 'networkidle2' })
    await sleep(2500)
    await page.screenshot({ path: join(OUT, '12-knife4j.png') })
    console.log('  📸 12-knife4j.png')
  } catch (e) { console.log('  knife4j 截图跳过', e.message) }

  await browser.close()
  console.log('全部截图完成 ->', OUT)
}
main().catch(e => { console.error(e); process.exit(1) })
