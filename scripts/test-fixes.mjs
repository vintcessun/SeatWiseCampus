const BASE = process.env.BASE || 'http://localhost:8888'
let pass = 0, fail = 0
const ok = (n, c, e = '') => { c ? (pass++, console.log(`  ✅ ${n} ${e}`)) : (fail++, console.log(`  ❌ ${n} ${e}`)) }
async function api(path, opt = {}) {
  const h = { 'Content-Type': 'application/json' }; if (opt.token) h['satoken'] = opt.token
  const r = await fetch(BASE + path, { method: opt.method || 'GET', headers: h, body: opt.body ? JSON.stringify(opt.body) : undefined })
  return await r.json().catch(() => ({}))
}
const login = async (u, p) => (await api('/api/auth/login', { method: 'POST', body: { username: u, password: p } }))
function tomorrow() { const d = new Date(); d.setDate(d.getDate() + 1); return d.toISOString().slice(0, 10) }

async function main() {
  console.log('\n== 修复项验证 ==\n')

  console.log('[1] BCrypt 登录（种子密码已迁移）')
  const l = await login('student1', '123456')
  ok('明文密码迁移后仍可登录', l.code === '0', 'token=' + (l.data?.token ? 'ok' : 'null'))
  const s1 = l.data?.token
  const adm = (await login('admin', 'admin123')).data?.token
  ok('管理员登录', !!adm)

  console.log('[2] 注册 + 自动登录')
  const uname = 'newstu_' + Date.now().toString().slice(-6)
  const reg = await api('/api/auth/register', { method: 'POST', body: { username: uname, password: 'abc123', realName: '新同学' } })
  ok('注册成功并返回登录态', reg.code === '0' && !!reg.data?.token, 'role=' + reg.data?.role)
  const dup = await api('/api/auth/register', { method: 'POST', body: { username: uname, password: 'abc123', realName: 'X' } })
  ok('重复用户名被拒', dup.code === 'USERNAME_EXISTS')
  const relogin = await login(uname, 'abc123')
  ok('新账号可用新密码登录（bcrypt）', relogin.code === '0')

  console.log('[3] AI 助手（DeepSeek LLM）')
  const ai = await api('/api/ai/assistant', { method: 'POST', token: s1, body: { message: '我想明天上午找个安静、靠窗、有插座的位置，坐三个小时' } })
  ok('AI 返回推荐', ai.code === '0' && ai.data?.recommendations?.length >= 0,
     'source=' + ai.data?.source + ' intent=' + JSON.stringify(ai.data?.intent))
  console.log('     reply:', ai.data?.reply)

  console.log('[4] 预约不能选已开始/过去时段')
  const past = await api('/api/reservations', { method: 'POST', token: s1, body: { roomId: 1, seatId: 5, date: new Date().toISOString().slice(0,10), startTime: '00:00', endTime: '00:30' } })
  ok('过去时段被拒', past.code === 'INVALID_TIME_RANGE', 'code=' + past.code)

  console.log('[5] 过早签到拦截')
  const T = tomorrow()
  const board = (await api(`/api/study-rooms/1/board?date=${T}&start=15:00&end=16:00`, { token: s1 })).data
  const seat = board.seats.find(x => x.status === 'FREE')
  const cr = await api('/api/reservations', { method: 'POST', token: s1, body: { roomId: 1, seatId: seat.seatId, date: T, startTime: '15:00', endTime: '16:00' } })
  ok('明日预约成功', cr.code === '0')
  const ci = await api(`/api/reservations/${cr.data.id}/check-in`, { method: 'POST', token: s1 })
  ok('未到时间签到被拒(SIGN_IN_TOO_EARLY)', ci.code === 'SIGN_IN_TOO_EARLY', 'code=' + ci.code)
  ok('预约含签到窗口字段', !!cr.data.signinStart && !!cr.data.signinDeadline, cr.data.signinStart + '-' + cr.data.signinDeadline)

  console.log('[6] 管理端按学生追踪预约')
  const track = await api('/api/admin/reservations?keyword=' + encodeURIComponent('张三'), { token: adm })
  ok('按姓名查到预约', track.code === '0' && track.data?.length > 0, '(' + track.data?.length + ' 条) 含姓名=' + (track.data?.[0]?.studentName))
  const denied = await api('/api/admin/reservations', { token: s1 })
  ok('学生访问管理接口被拒', denied.code === 'PERMISSION_DENIED')

  console.log('[7] 管理端生成自定义行列布局')
  const gen = await api('/api/study-rooms/2/generate-layout?rows=4&cols=5&aisleCol=2', { method: 'POST', token: adm })
  ok('生成 4×5 布局', gen.code === '0')
  const layout = (await api('/api/study-rooms/2/layout', { token: adm })).data
  ok('布局回显行列正确', layout.rows === 4 && layout.cols === 5, layout.rows + '×' + layout.cols)

  console.log(`\n== 结果: ${pass} 通过 / ${fail} 失败 ==\n`)
  process.exit(fail === 0 ? 0 : 1)
}
main().catch(e => { console.error(e); process.exit(1) })
