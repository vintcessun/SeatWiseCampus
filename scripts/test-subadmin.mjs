// 子管理员测试：主管理员创建子管理员；子管理员可用常规管理端能力但不能管理管理员
const BASE = 'http://localhost:8888/api'
let pass = 0, fail = 0
function ok(name, cond, extra = '') { if (cond) { pass++; console.log(`  ✓ ${name}`) } else { fail++; console.log(`  ✗ ${name} ${extra}`) } }
async function api(path, { method = 'GET', token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['satoken'] = token
  const res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  return res.json().catch(() => ({}))
}
const login = async (u, p) => api('/auth/login', { method: 'POST', body: { username: u, password: p } })

;(async () => {
  console.log('子管理员（R6）测试')
  const admin = (await login('admin', 'admin123')).data.token

  // 主管理员可访问管理员管理
  const listed = await api('/admin/admins', { token: admin })
  ok('主管理员可列出管理员', listed.code === '0' && Array.isArray(listed.data), JSON.stringify(listed).slice(0, 80))

  // 创建子管理员
  const uname = 'subadmin_' + Date.now().toString().slice(-6)
  const created = await api('/admin/admins', { method: 'POST', token: admin, body: { username: uname, password: 'sub123', realName: '楼层管理员' } })
  ok('创建子管理员成功', created.code === '0', JSON.stringify(created).slice(0, 80))
  const subId = created.data.id

  // 子管理员登录
  const subLogin = await login(uname, 'sub123')
  ok('子管理员可登录', subLogin.code === '0' && subLogin.data.role === 'ADMIN_SUB', JSON.stringify(subLogin.data?.role))
  const sub = subLogin.data.token

  // 子管理员可用常规管理端能力（如报表 / 学生追踪）
  const rep = await api('/reports/summary', { token: sub })
  ok('子管理员可查看报表', rep.code === '0', JSON.stringify(rep).slice(0, 60))
  const track = await api('/admin/reservations?keyword=张', { token: sub })
  ok('子管理员可追踪学生预约', track.code === '0')

  // 子管理员不能管理管理员（SUPER 限制）
  const denyList = await api('/admin/admins', { token: sub })
  ok('子管理员不能列出管理员(403)', denyList.code === 'PERMISSION_DENIED' || denyList.code === '403', JSON.stringify(denyList).slice(0, 80))
  const denyCreate = await api('/admin/admins', { method: 'POST', token: sub, body: { username: 'x' + uname, password: 'sub123', realName: 'X' } })
  ok('子管理员不能创建管理员(403)', denyCreate.code === 'PERMISSION_DENIED' || denyCreate.code === '403')

  // 主管理员删除子管理员
  const del = await api(`/admin/admins/${subId}`, { method: 'DELETE', token: admin })
  ok('主管理员可删除子管理员', del.code === '0', JSON.stringify(del).slice(0, 60))
  const relogin = await login(uname, 'sub123')
  ok('删除后子管理员无法登录', relogin.code !== '0')

  // 不能删除主管理员
  const admins = (await api('/admin/admins', { token: admin })).data
  const primary = admins.find(a => a.primary)
  const delPrimary = await api(`/admin/admins/${primary.id}`, { method: 'DELETE', token: admin })
  ok('不能删除主管理员', delPrimary.code !== '0', JSON.stringify(delPrimary).slice(0, 60))

  console.log(`\n结果：${pass} 通过 / ${fail} 失败`)
  process.exit(fail ? 1 : 0)
})().catch(e => { console.error('测试异常', e); process.exit(1) })
