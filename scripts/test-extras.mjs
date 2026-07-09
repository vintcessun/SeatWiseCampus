// 附加功能覆盖：积分明细、AI 助手、附近空位、冲突替代、报表、管理端追踪、黑名单
const BASE = 'http://localhost:8888/api'
let pass = 0, fail = 0
function ok(name, cond, extra = '') { if (cond) { pass++; console.log(`  ✓ ${name}`) } else { fail++; console.log(`  ✗ ${name} ${extra}`) } }
async function api(path, { method = 'GET', token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['satoken'] = token
  const res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  return { status: res.status, j: await res.json().catch(() => ({})) }
}
const login = async (u, p) => (await api('/auth/login', { method: 'POST', body: { username: u, password: p } })).j.data
const today = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` }

;(async () => {
  console.log('附加功能覆盖测试')
  const date = today()
  const stu = await login('student1', '123456')
  const adm = await login('admin', 'admin123')

  // 1) 我的积分（明细）—— 曾因 score_change 保留字问题 500，回归覆盖
  const sme = await api('/scores/me', { token: stu.token })
  ok('我的积分明细 /scores/me', sme.j.code === '0' && sme.j.data && Array.isArray(sme.j.data.records), JSON.stringify(sme.j).slice(0, 140))
  ok('积分明细含 creditScore', sme.j.data && typeof sme.j.data.creditScore !== 'undefined')

  // 2) 积分排行
  const rank = await api('/scores/ranking?period=all', { token: stu.token })
  ok('积分排行榜', rank.j.code === '0' && Array.isArray(rank.j.data))

  // 3) AI 助手
  const ai = await api('/ai/assistant', { method: 'POST', token: stu.token, body: { message: '帮我找个安静靠窗的座位' } })
  ok('AI 助手返回', ai.j.code === '0' && ai.j.data, JSON.stringify(ai.j).slice(0, 120))

  // 4) 附近空位（正确参数 originBuildingId）
  const buildings = await api('/buildings?campusId=1', { token: stu.token })
  const bid = buildings.j.data?.[0]?.id
  const near = await api(`/rooms/nearest-available?originBuildingId=${bid}&date=${date}&start=16:00&end=17:00`, { token: stu.token })
  ok('附近空位推荐', near.j.code === '0' && Array.isArray(near.j.data), JSON.stringify(near.j).slice(0, 120))

  // 5) 冲突智能替代
  const roomId = (await api('/study-rooms?campusId=1', { token: stu.token })).j.data[0].id
  const alt = await api(`/rooms/alternatives?roomId=${roomId}&date=${date}&start=16:00&end=17:00`, { token: stu.token })
  ok('冲突智能替代', alt.j.code === '0' && Array.isArray(alt.j.data))

  // 6) 报表
  const rep = await api('/reports/summary', { token: adm.token })
  ok('数据报表 summary', rep.j.code === '0' && rep.j.data)

  // 7) 管理端学生预约追踪
  const track = await api('/admin/reservations?keyword=张', { token: adm.token })
  ok('管理端预约追踪(按姓名)', track.j.code === '0' && Array.isArray(track.j.data))

  // 8) 黑名单列表
  const bl = await api('/admin/blacklist', { token: adm.token })
  ok('黑名单列表', bl.j.code === '0')

  // 9) 我的黑名单状态
  const blme = await api('/blacklist/me', { token: stu.token })
  ok('我的黑名单状态', blme.j.code === '0')

  // 10) 通知未读数
  const un = await api('/notifications/unread-count', { token: stu.token })
  ok('通知未读数', un.j.code === '0')

  console.log(`\n结果：${pass} 通过 / ${fail} 失败`)
  process.exit(fail ? 1 : 0)
})().catch(e => { console.error('测试异常', e); process.exit(1) })
