import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  { path: '/register', component: () => import('../views/Register.vue'), meta: { public: true } },
  {
    path: '/student',
    component: () => import('../layouts/StudentLayout.vue'),
    meta: { role: 'STUDENT' },
    children: [
      { path: '', redirect: '/student/home' },
      { path: 'home', component: () => import('../views/student/Home.vue') },
      { path: 'rooms', component: () => import('../views/student/Rooms.vue') },
      { path: 'rooms/:roomId/seats', component: () => import('../views/student/Seats.vue') },
      { path: 'spacetime', component: () => import('../views/student/Spacetime.vue') },
      { path: 'reservations', component: () => import('../views/student/Reservations.vue') },
      { path: 'waitlist', component: () => import('../views/student/Waitlist.vue') },
      { path: 'report', component: () => import('../views/student/StudyReport.vue') },
      { path: 'pomodoro', component: () => import('../views/student/Pomodoro.vue') },
      { path: 'ranking', component: () => import('../views/student/Ranking.vue') },
      { path: 'nearby', component: () => import('../views/student/Nearby.vue') }
    ]
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { role: 'ADMIN' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: () => import('../views/admin/Home.vue') },
      { path: 'rooms', component: () => import('../views/admin/Rooms.vue') },
      { path: 'rooms/:roomId/layout', component: () => import('../views/admin/LayoutEditor.vue') },
      { path: 'rooms/:roomId/board', component: () => import('../views/admin/Board.vue') },
      { path: 'rooms/:roomId/replay', component: () => import('../views/admin/Replay.vue') },
      { path: 'spacetime', component: () => import('../views/student/Spacetime.vue') },
      { path: 'students', component: () => import('../views/admin/StudentTracking.vue') },
      { path: 'reports', component: () => import('../views/admin/Reports.vue') },
      { path: 'announcements', component: () => import('../views/admin/Announcements.vue') },
      { path: 'locations', component: () => import('../views/admin/Locations.vue') },
      { path: 'blacklist', component: () => import('../views/admin/Blacklist.vue') },
      { path: 'admins', component: () => import('../views/admin/Admins.vue') }
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
  const isAdmin = role === 'ADMIN' || role === 'ADMIN_SUB'
  if (to.meta.public) return true
  if (!token) return '/login'
  if (to.meta.role === 'ADMIN' && !isAdmin) return '/student'
  if (to.meta.role === 'STUDENT' && isAdmin) return '/admin'
  return true
})

export default router
