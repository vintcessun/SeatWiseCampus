import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  {
    path: '/student',
    component: () => import('../layouts/StudentLayout.vue'),
    meta: { role: 'STUDENT' },
    children: [
      { path: '', redirect: '/student/rooms' },
      { path: 'rooms', component: () => import('../views/student/Rooms.vue') },
      { path: 'rooms/:roomId/seats', component: () => import('../views/student/Seats.vue') },
      { path: 'reservations', component: () => import('../views/student/Reservations.vue') },
      { path: 'ranking', component: () => import('../views/student/Ranking.vue') },
      { path: 'nearby', component: () => import('../views/student/Nearby.vue') }
    ]
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { role: 'ADMIN' },
    children: [
      { path: '', redirect: '/admin/rooms' },
      { path: 'rooms', component: () => import('../views/admin/Rooms.vue') },
      { path: 'rooms/:roomId/layout', component: () => import('../views/admin/LayoutEditor.vue') },
      { path: 'rooms/:roomId/board', component: () => import('../views/admin/Board.vue') },
      { path: 'reports', component: () => import('../views/admin/Reports.vue') },
      { path: 'blacklist', component: () => import('../views/admin/Blacklist.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('satoken')
  const role = localStorage.getItem('role')
  if (to.meta.public) return true
  if (!token) return '/login'
  if (to.meta.role && to.meta.role !== role) {
    return role === 'ADMIN' ? '/admin' : '/student'
  }
  return true
})

export default router
