// 下层 · 候补队列自动补位闭环（对齐 scripts/test-waitlist.mjs 的可靠流程）：
//   A 预约某座位 → B 加入该时段候补 → A 取消 → 被释放的座位自动保留 60s 给 B（OFFERED）+ 站内通知。
// 默认不自动 accept，把"立即确认预约"留给 Playwright/前端点击更有观感；ACCEPT=1 时脚本兜底确认。
// 建议干净库运行。用法： node waitlist-demo.mjs
//        HOLDER=student1 WAITER=student2 SLOT=14:00-16:00 ACCEPT=0 node waitlist-demo.mjs
import {
  login, firstRoomId, freeSeats, reserve, cancel, joinWaitlist, waitlistMine, waitlistAccept,
  notifications, tomorrow, banner, step, good, bad, info, sleep, isMain,
} from './lib.mjs'

export async function waitlistDemo(opts = {}) {
  const env = process.env
  const date = opts.date || env.DATE || tomorrow()
  const [start, end] = (opts.slot || env.SLOT || '14:00-16:00').split('-')
  const holderName = opts.holder || env.HOLDER || 'student1'
  const waiterName = opts.waiter || env.WAITER || 'student2'
  const doAccept = String(opts.accept ?? env.ACCEPT ?? '0') !== '0'

  banner('候补队列 waitlist-demo')
  const holder = await login(holderName, '123456')
  const waiter = await login(waiterName, '123456')
  const roomId = opts.roomId || Number(env.ROOM) || (await firstRoomId(holder))
  const win = { date, start, end }
  info(`房间=${roomId} 日期=${date} 时段=${start}-${end} 占座=${holderName} 候补=${waiterName}`)

  // 1) holder 预约一个座位
  const seat = (await freeSeats(roomId, win, holder, 1))[0]
  if (!seat) { bad('无空位可预约'); return {} }
  const r = await reserve(holder, { roomId, seatId: seat.seatId, ...win })
  if (r.code !== '0') { bad(`占座失败：${r.code}`); return {} }
  good(`${holderName} 预约座位 ${seat.seatNo}`)

  // 2) waiter 加入候补（API 允许在有空位时也加入；前端仅在满员时展示入口）
  step(`${waiterName} 加入候补…`)
  const wj = await joinWaitlist(waiter, { roomId, ...win })
  if (wj.code !== '0') { bad(`加入候补失败：${wj.code}`); return {} }
  let mine = await waitlistMine(waiter)
  good(`候补状态：${mine[0]?.status}`)

  // 3) holder 取消 → 触发 onSeatReleased 自动保留 60s
  step(`${holderName} 取消预约以触发自动补位…`)
  await cancel(holder, r.data.id)

  // 4) 轮询候补状态，等待 OFFERED
  let offer = null
  for (let i = 0; i < 12; i++) {
    await sleep(500)
    mine = await waitlistMine(waiter)
    if (mine[0]?.status === 'OFFERED') { offer = mine[0]; break }
  }
  if (!offer) { bad('未在预期时间内收到 OFFERED'); return {} }
  good(`候补被自动保留：座位 ${offer.offeredSeatId}（=被释放的 ${seat.seatId}），状态 OFFERED · 约 60s 内确认`)
  const notis = await notifications(waiter)
  info('候补者通知类型：' + JSON.stringify(notis.map((n) => n.type)))

  // 5) 确认（默认交给前端点击；ACCEPT=1 时脚本兜底）
  if (doAccept) {
    const acc = await waitlistAccept(waiter, offer.id)
    good(acc.code === '0' ? '脚本已确认候补 → 生成正式预约' : `确认失败：${acc.code}`)
  } else {
    info('未自动确认：请在 Playwright/前端「我的候补」页点「立即确认预约」（60s 内）')
  }
  return { roomId, date, start, end, offerId: offer.id, offeredSeatId: offer.offeredSeatId }
}

if (isMain(import.meta.url)) waitlistDemo().catch((e) => { console.error(e); process.exit(1) })
