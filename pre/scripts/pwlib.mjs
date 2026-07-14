// 上层 · Playwright 通用库（Edge via playwright-core，无需下载 Chromium）。
// 登录用 localStorage 注入（与语言无关，规避验证码）；导航用直达路由；
// 座位选择/状态观察基于 SeatGrid 的稳定 class（.seat-cell.seat-FREE.clickable 等）。
import { chromium } from 'playwright-core'
import { loginFull, BASE as WEB } from './lib.mjs'

export const HEADLESS = String(process.env.HEADLESS ?? '0') === '1'
export const SLOWMO = Number(process.env.SLOWMO ?? (HEADLESS ? 0 : 350))
export const CHANNEL = process.env.PW_CHANNEL || 'msedge'

// 启动一个独立浏览器窗口（便于录制时并排摆放两个窗口）
export async function launchBrowser({ label = 'win', x = 0, y = 0, w = 960, h = 1040 } = {}) {
  const args = [`--window-position=${x},${y}`, `--window-size=${w},${h}`, '--lang=zh-CN']
  const browser = await chromium.launch({ channel: CHANNEL, headless: HEADLESS, slowMo: SLOWMO, args })
  browser._swLabel = label
  return browser
}

// 在指定浏览器里创建一个已注入登录态的页面
export async function newInjectedPage(browser, account) {
  const auth = await loginFull(account.username, account.password) // { token, role, userInfo }
  const ctx = await browser.newContext(HEADLESS ? { viewport: { width: 1440, height: 1000 } } : { viewport: null })
  await ctx.addInitScript((a) => {
    localStorage.setItem('satoken', a.token)
    localStorage.setItem('role', a.role)
    localStorage.setItem('userInfo', JSON.stringify(a.userInfo))
  }, auth)
  const page = await ctx.newPage()
  page._swAuth = auth
  return page
}

// ---- 导航 ----
export async function gotoSeats(page, roomId, { date, start, end }) {
  const q = new URLSearchParams({ date, start, end })
  await page.goto(`${WEB}/student/rooms/${roomId}/seats?${q}`, { waitUntil: 'domcontentloaded' })
  await page.locator('.unified-grid').first().waitFor({ timeout: 15000 })
}
export async function gotoAdminBoard(page, roomId, { date, start, end }) {
  const q = new URLSearchParams({ date, start, end })
  await page.goto(`${WEB}/admin/rooms/${roomId}/board?${q}`, { waitUntil: 'domcontentloaded' })
  await page.locator('.unified-grid').first().waitFor({ timeout: 15000 })
}
export const gotoPath = (page, path) => page.goto(`${WEB}${path}`, { waitUntil: 'domcontentloaded' })

// 可见表单登录（不依赖 i18n 文案：按输入框顺序填，点主按钮）。用于「登录」幕的真实演示。
export async function formLogin(page, username, password) {
  await page.goto(`${WEB}/login`, { waitUntil: 'domcontentloaded' })
  const inputs = page.locator('.login-card input')
  await inputs.nth(0).waitFor({ timeout: 10000 })
  await inputs.nth(0).fill(username)
  await inputs.nth(1).fill(password)
  await page.locator('.login-card .el-button--primary').first().click()
  await page.waitForURL(/\/(student|admin)/, { timeout: 15000 })
}

// ---- 座位网格操作/观察 ----
export const freeSeatLoc = (page) => page.locator('.seat-cell.seat-FREE.clickable')
export const countClass = (page, cls) => page.locator(`.seat-cell.${cls}`).count()
export async function clickFirstFreeSeat(page) {
  const loc = freeSeatLoc(page).first()
  await loc.waitFor({ timeout: 10000 })
  await loc.click()
}
// 点确认预约对话框里的「确认预约」按钮
export async function confirmReserve(page) {
  const dlg = page.locator('.el-dialog:has-text("确认预约")')
  await dlg.waitFor({ timeout: 8000 })
  await dlg.getByRole('button', { name: '确认预约' }).click()
  // 成功后对话框关闭
  await dlg.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {})
}
// 等待某状态座位数量 >= n（用于观察另一窗口的 SSE 联动）
export async function waitClassAtLeast(page, cls, n, timeout = 15000) {
  await page.waitForFunction(
    ({ cls, n }) => document.querySelectorAll(`.seat-cell.${cls}`).length >= n,
    { cls, n }, { timeout },
  )
}
export const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
