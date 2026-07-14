// 下层 · 组队相邻原子预约：先演功能闭环（一组多座同排相邻整体成功），
// 再演并发原子性（两组抢重叠相邻座，恰好一组整体成功、败组回滚）。
// 建议干净库运行。用法： node group-demo.mjs
import {
  login, firstRoomId, board, reserveGroup, tomorrow,
  banner, step, good, bad, info, isMain,
} from './lib.mjs'

// 在看板中找同一排连续 count 个空位
function findRun(seats, count) {
  const free = seats.filter((s) => s.cellType === 'SEAT' && s.status === 'FREE')
  const byRow = {}
  for (const s of free) (byRow[s.rowIndex] ||= []).push(s)
  for (const r of Object.keys(byRow)) {
    const arr = byRow[r].sort((a, b) => a.colIndex - b.colIndex)
    for (let i = 0; i + count - 1 < arr.length; i++) {
      let ok = true
      for (let k = 1; k < count; k++) if (arr[i + k].colIndex !== arr[i].colIndex + k) { ok = false; break }
      if (ok) return arr.slice(i, i + count)
    }
  }
  return null
}

export async function groupDemo(opts = {}) {
  const env = process.env
  const date = opts.date || env.DATE || tomorrow()
  banner('组队相邻原子预约 group-demo')

  const t = {}
  for (let i = 1; i <= 6; i++) t['s' + i] = await login('student' + i, '123456')
  const roomId = opts.roomId || Number(env.ROOM) || (await firstRoomId(t.s1))

  // === 1) 功能闭环：3 座组队成功（时段 A）===
  // 用清晨独立时段，避免与主演示的 14:00-22:00 时段重叠导致 RESERVATION_TIME_CONFLICT
  const winA = { date, start: '09:00', end: '10:00' }
  const bA = await board(roomId, winA, t.s1)
  const runA = findRun(bA.seats, 4)
  if (!runA) { bad('未找到 4 个同排连续空位（换干净库/换房间）'); return {} }
  const [a, b, c, d] = runA
  step(`时段 ${winA.start}-${winA.end} 组队 3 座：${a.seatNo},${b.seatNo},${c.seatNo}`)
  const g1 = await reserveGroup(t.s1, {
    roomId, ...winA,
    members: [
      { seatId: a.seatId, username: 'student1' },
      { seatId: b.seatId, username: 'student2' },
      { seatId: c.seatId, username: 'student3' },
    ],
  })
  if (g1.code === '0' && (g1.data || []).length === 3) good('组队预约成功（3 座整体锁定）')
  else bad(`组队失败：${JSON.stringify(g1).slice(0, 160)}`)

  // 非相邻应被拒
  const nonAdj = await reserveGroup(t.s4, {
    roomId, ...winA,
    members: [{ seatId: a.seatId, username: 'student4' }, { seatId: d.seatId, username: 'student5' }],
  })
  info(nonAdj.code !== '0' ? '✓ 非相邻座位被拒绝（校验生效）' : '✗ 非相邻竟成功（异常）')

  // === 2) 并发原子性：两组抢重叠相邻座（时段 B），恰好一组整体成功 ===
  const winB = { date, start: '10:00', end: '11:00' }
  const bB = await board(roomId, winB, t.s1)
  const runB = findRun(bB.seats, 3)
  if (!runB) { bad('时段 B 未找到 3 连续空位'); return {} }
  const [x, y, z] = runB
  step(`时段 ${winB.start}-${winB.end} 两组同时抢：组A{${x.seatNo},${y.seatNo}} vs 组B{${y.seatNo},${z.seatNo}}（重叠 ${y.seatNo}）`)
  const teamA = reserveGroup(t.s1, { roomId, ...winB, members: [{ seatId: x.seatId, username: 'student1' }, { seatId: y.seatId, username: 'student2' }] })
  const teamB = reserveGroup(t.s3, { roomId, ...winB, members: [{ seatId: y.seatId, username: 'student3' }, { seatId: z.seatId, username: 'student4' }] })
  const [ra, rb] = await Promise.all([teamA, teamB])
  const successes = [ra, rb].filter((r) => r.code === '0').length
  if (successes === 1) good(`并发两组恰好一组整体成功（A=${ra.code} B=${rb.code}）`)
  else bad(`期望恰好 1 组成功，实际 ${successes}（A=${ra.code} B=${rb.code}）——需干净库`)

  // 校验败组独占座未被误占（原子回滚）
  const after = (await board(roomId, winB, t.s1)).seats
  const st = (id) => after.find((s) => s.seatId === id)?.status
  const winnerIsA = ra.code === '0'
  const loserSoloFree = winnerIsA ? st(z.seatId) === 'FREE' : st(x.seatId) === 'FREE'
  info(loserSoloFree ? '✓ 败组独占座仍空闲（整单原子回滚）' : '✗ 败组独占座被误占')
  return { roomId, date, winnerIsA }
}

if (isMain(import.meta.url)) groupDemo().catch((e) => { console.error(e); process.exit(1) })
