// SeatWise Campus 演示框架 · 下层通用 API 库（同目录共享，不跨目录引用）
// 范式对齐 scripts/seed-demo.mjs 等：fetch + satoken 头 + { code, data } 约定。
// Node 18+ 内置 fetch，无需三方依赖。

export const BASE = process.env.BASE || 'http://localhost:8888'

// ---- 演示账号（与 RUN.md 一致）----
export const ADMIN = { username: 'admin', password: 'admin123' }
export const STUDENTS = Array.from({ length: 8 }, (_, i) => ({
  username: `student${i + 1}`, password: '123456',
}))

// ---- 基础请求 ----
export async function api(path, { method = 'GET', token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['satoken'] = token // REST 走请求头
  const res = await fetch(BASE + path, {
    method, headers, body: body ? JSON.stringify(body) : undefined,
  })
  const json = await res.json().catch(() => ({}))
  return json // { code, message, data }
}
export const ok = (r) => r && r.code === '0'

// ---- 登录 ----
export async function loginFull(username, password) {
  const r = await api('/api/auth/login', { method: 'POST', body: { username, password } })
  if (!ok(r)) throw new Error(`登录失败 ${username}: ${JSON.stringify(r)}`)
  return r.data // { token, role, userInfo }
}
export const login = async (u, p) => (await loginFull(u, p)).token

// ---- 日期/时间 ----
export const pad2 = (n) => String(n).padStart(2, '0')
export function tomorrow() { const d = new Date(); d.setDate(d.getDate() + 1); return isoDate(d) }
export function today() { return isoDate(new Date()) }
export const isoDate = (d) => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
export const hhmm = (min) => `${pad2(Math.floor(min / 60) % 24)}:${pad2(min % 60)}`
export const toMin = (s) => { const [h, m] = s.split(':').map(Number); return h * 60 + m }

// ---- 领域封装 ----
export async function firstCampusId(token) {
  const r = await api('/api/campuses', { token })
  return r.data?.[0]?.id
}
export async function firstRoomId(token) {
  const r = await api('/api/study-rooms', { token })
  return r.data?.[0]?.id
}
export async function board(roomId, { date, start, end }, token) {
  const q = new URLSearchParams({ date, ...(start ? { start } : {}), ...(end ? { end } : {}) })
  const r = await api(`/api/study-rooms/${roomId}/board?${q}`, { token })
  return r.data // { seats:[{seatId,seatNo,rowIndex,colIndex,cellType,tags,status,...}], rows, cols, ... }
}
export async function freeSeats(roomId, win, token, n = Infinity) {
  const b = await board(roomId, win, token)
  return (b.seats || [])
    .filter((s) => s.cellType === 'SEAT' && s.status === 'FREE')
    .slice(0, n)
}
export const reserve = (token, { roomId, seatId, date, start, end }) =>
  api('/api/reservations', { method: 'POST', token, body: { roomId: Number(roomId), seatId, date, startTime: start, endTime: end } })
export const reserveGroup = (token, { roomId, date, start, end, members }) =>
  api('/api/reservations/group', { method: 'POST', token, body: { roomId: Number(roomId), date, startTime: start, endTime: end, members } })
export const myReservations = (token) => api('/api/reservations/me', { token }).then((r) => r.data || [])
export const checkIn = (token, id) => api(`/api/reservations/${id}/check-in`, { method: 'POST', token })
export const checkOut = (token, id) => api(`/api/reservations/${id}/check-out`, { method: 'POST', token })
export const cancel = (token, id) => api(`/api/reservations/${id}/cancel`, { method: 'POST', token })

export const hold = (token, { roomId, seatId, date, start, end }) =>
  api('/api/holds', { method: 'POST', token, body: { roomId: Number(roomId), seatId, date, startTime: start, endTime: end } })
export const releaseHold = (token, { roomId, seatId, date }) =>
  api('/api/holds/release', { method: 'POST', token, body: { roomId: Number(roomId), seatId, date } })

export const joinWaitlist = (token, { roomId, date, start, end }) =>
  api('/api/waitlist', { method: 'POST', token, body: { roomId: Number(roomId), date, startTime: start, endTime: end } })
export const waitlistMine = (token) => api('/api/waitlist/me', { token }).then((r) => r.data || [])
export const waitlistAccept = (token, id) => api(`/api/waitlist/${id}/accept`, { method: 'POST', token })
export const waitlistCancel = (token, id) => api(`/api/waitlist/${id}/cancel`, { method: 'POST', token })

export const notifications = (token) => api('/api/notifications', { token }).then((r) => r.data || [])

// ---- 时间窗工具：默认取「明天 14:00-16:00」，保证 start > now ----
export function defaultWindow() {
  return { date: tomorrow(), start: '14:00', end: '16:00' }
}

// ---- 控制台美化 ----
export const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
export function banner(title) { console.log(`\n\x1b[36m══ ${title} ══\x1b[0m`) }
export function step(msg) { console.log(`\x1b[90m→\x1b[0m ${msg}`) }
export function good(msg) { console.log(`  \x1b[32m✓\x1b[0m ${msg}`) }
export function bad(msg) { console.log(`  \x1b[31m✗\x1b[0m ${msg}`) }
export function info(msg) { console.log(`  \x1b[33m•\x1b[0m ${msg}`) }

// 直接运行判断（供各脚本 CLI 入口，兼容 Windows 空格/盘符）
import { pathToFileURL } from 'node:url'
export function isMain(importMetaUrl) {
  if (!process.argv[1]) return false
  return importMetaUrl === pathToFileURL(process.argv[1]).href
}
