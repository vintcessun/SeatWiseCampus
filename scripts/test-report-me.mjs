// 个人自习报告测试：字段完整性 + 聚合口径（基于 DataInitializer 注入的历史数据）
const BASE = 'http://localhost:8888/api'
let pass = 0, fail = 0
function ok(name, cond, extra = '') { if (cond) { pass++; console.log(`  ✓ ${name}`) } else { fail++; console.log(`  ✗ ${name} ${extra}`) } }
async function api(path, { method = 'GET', token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['satoken'] = token
  const res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  return res.json().catch(() => ({}))
}
const login = async (u, p) => (await api('/auth/login', { method: 'POST', body: { username: u, password: p } })).data.token

;(async () => {
  console.log('个人自习报告测试')
  // 汇总所有学生的报告，验证口径；至少有人有完成记录（来自 DataInitializer 历史注入）
  let anyCompleted = false, anyExpired = false, allShapesOk = true, allRateValid = true, weeklyOk = true
  for (let i = 1; i <= 8; i++) {
    const t = await login('student' + i, '123456')
    const r = await api('/me/study-report', { token: t })
    if (r.code !== '0' || !r.data) { allShapesOk = false; continue }
    const d = r.data
    const keys = ['completedSessions', 'expiredSessions', 'totalHours', 'onTimeRate', 'streakDays', 'weekly']
    if (!keys.every(k => k in d)) allShapesOk = false
    if (!Array.isArray(d.weekly) || d.weekly.length !== 7) weeklyOk = false
    if (typeof d.onTimeRate !== 'number' || d.onTimeRate < 0 || d.onTimeRate > 100) allRateValid = false
    if (d.completedSessions > 0) {
      anyCompleted = true
      // 完成>0 则累计时长应>0
      if (!(d.totalHours > 0)) allShapesOk = false
      // 近7天时长求和不应超过累计时长（近7天是子集）
      const wsum = d.weekly.reduce((a, w) => a + w.hours, 0)
      if (wsum - d.totalHours > 0.5) allShapesOk = false
    }
    if (d.expiredSessions > 0) anyExpired = true
  }
  ok('所有学生报告结构完整', allShapesOk)
  ok('近 7 天数组长度为 7', weeklyOk)
  ok('守约率均在 [0,100]', allRateValid)
  ok('存在完成场次（历史注入生效）', anyCompleted)
  ok('存在爽约释放记录（守约率<100 可体现）', anyExpired)

  // 精确口径：新注册用户报告应全 0 且守约率 100
  const uname = 'reportzero_' + Date.now().toString().slice(-8)
  const cap = (await api('/captcha')).data
  const reg = await api('/auth/register', { method: 'POST', body: { username: uname, password: '123456', realName: '零报告', captchaId: cap.captchaId, captchaCode: cap.code } })
  const fresh = reg.data?.token
  const zr = (await api('/me/study-report', { token: fresh })).data
  ok('新用户完成场次为 0', zr.completedSessions === 0)
  ok('新用户守约率默认 100', zr.onTimeRate === 100)
  ok('新用户连续天数为 0', zr.streakDays === 0)

  console.log(`\n结果：${pass} 通过 / ${fail} 失败`)
  process.exit(fail ? 1 : 0)
})().catch(e => { console.error('测试异常', e); process.exit(1) })
