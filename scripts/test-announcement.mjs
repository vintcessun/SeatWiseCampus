// 公告中心测试：发布→学生可见→（推送）通知+1→下线不可见→删除
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
  console.log('公告中心测试')
  const adm = await login('admin', 'admin123')
  const stu = await login('student3', '123456')

  const seed = await api('/announcements', { token: stu })
  ok('学生可见种子公告(≥2)', seed.code === '0' && (seed.data || []).length >= 2, JSON.stringify(seed).slice(0, 100))

  const before = (await api('/notifications', { token: stu })).data || []
  const beforeAnn = before.filter(n => n.type === 'ANNOUNCEMENT').length

  const created = await api('/admin/announcements', { method: 'POST', token: adm, body: {
    title: '自动化测试公告', content: '这是一条测试公告内容', level: 'WARN', notifyAll: true } })
  ok('管理员发布成功', created.code === '0' && created.data?.id, JSON.stringify(created).slice(0, 100))
  const id = created.data.id

  const listStu = await api('/announcements', { token: stu })
  ok('学生端可见新公告', (listStu.data || []).some(a => a.id === id))

  const afterN = (await api('/notifications', { token: stu })).data || []
  ok('学生收到 ANNOUNCEMENT 推送(+1)', afterN.filter(n => n.type === 'ANNOUNCEMENT').length === beforeAnn + 1,
    `before=${beforeAnn} after=${afterN.filter(n => n.type === 'ANNOUNCEMENT').length}`)

  // 权限：学生不能发公告
  const denied = await api('/admin/announcements', { method: 'POST', token: stu, body: { title: 'x', content: 'y' } })
  ok('学生发布被拒(403)', denied.code === 'PERMISSION_DENIED' || denied.code === '403', JSON.stringify(denied).slice(0, 80))

  // 下线
  await api(`/admin/announcements/${id}`, { method: 'PUT', token: adm, body: { active: 0 } })
  const afterOff = await api('/announcements', { token: stu })
  ok('下线后学生端不可见', !(afterOff.data || []).some(a => a.id === id))
  const adminAll = await api('/admin/announcements', { token: adm })
  ok('管理员全量仍含已下线', (adminAll.data || []).some(a => a.id === id && a.active === 0))

  // 编辑
  await api(`/admin/announcements/${id}`, { method: 'PUT', token: adm, body: { title: '改后标题', active: 1 } })
  const edited = (await api('/admin/announcements', { token: adm })).data.find(a => a.id === id)
  ok('编辑标题生效', edited?.title === '改后标题')

  // 删除
  await api(`/admin/announcements/${id}`, { method: 'DELETE', token: adm })
  const afterDel = (await api('/admin/announcements', { token: adm })).data || []
  ok('删除后不存在', !afterDel.some(a => a.id === id))

  console.log(`\n结果：${pass} 通过 / ${fail} 失败`)
  process.exit(fail ? 1 : 0)
})().catch(e => { console.error('测试异常', e); process.exit(1) })
