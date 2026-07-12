import http from './http'

export const authApi = {
  login: (data) => http.post('/auth/login', data),
  register: (data) => http.post('/auth/register', data),
  logout: () => http.post('/auth/logout'),
  me: () => http.get('/users/me'),
  captcha: () => http.get('/captcha'),
  resetPassword: (data) => http.post('/auth/reset-password', data)
}

export const baseApi = {
  campuses: () => http.get('/campuses'),
  buildings: (campusId) => http.get('/buildings', { params: { campusId } }),
  rooms: (params) => http.get('/study-rooms', { params }),
  layout: (roomId) => http.get(`/study-rooms/${roomId}/layout`),
  saveLayout: (roomId, data) => http.put(`/study-rooms/${roomId}/layout`, data),
  toggleSeat: (seatId, enabled) => http.post(`/seats/${seatId}/toggle`, null, { params: { enabled } }),
  generateLayout: (roomId, params) => http.post(`/study-rooms/${roomId}/generate-layout`, null, { params }),
  createCampus: (data) => http.post('/campuses', data),
  createBuilding: (data) => http.post('/buildings', data),
  updateBuildingLocation: (id, latitude, longitude) => http.put(`/buildings/${id}/location`, null, { params: { latitude, longitude } }),
  createRoom: (data) => http.post('/study-rooms', data),
  deleteRoom: (roomId) => http.delete(`/study-rooms/${roomId}`),
  setRoomStatus: (roomId, status) => http.post(`/study-rooms/${roomId}/status`, null, { params: { status } })
}

export const adminApi = {
  reservations: (params) => http.get('/admin/reservations', { params })
}

export const adminUsersApi = {
  list: () => http.get('/admin/admins'),
  create: (data) => http.post('/admin/admins', data),
  remove: (id) => http.delete(`/admin/admins/${id}`)
}

export const boardApi = {
  snapshot: (roomId, params) => http.get(`/study-rooms/${roomId}/board`, { params }),
  replay: (roomId, date) => http.get(`/study-rooms/${roomId}/replay`, { params: { date } })
}

export const holdApi = {
  hold: (data) => http.post('/holds', data),
  release: (data) => http.post('/holds/release', data)
}

export const reservationApi = {
  create: (data) => http.post('/reservations', data),
  group: (data) => http.post('/reservations/group', data),
  mine: () => http.get('/reservations/me'),
  checkIn: (id) => http.post(`/reservations/${id}/check-in`),
  checkOut: (id) => http.post(`/reservations/${id}/check-out`),
  cancel: (id) => http.post(`/reservations/${id}/cancel`)
}

export const reportApi = {
  summary: () => http.get('/reports/summary')
}

export const scoreApi = {
  me: () => http.get('/scores/me'),
  ranking: (period) => http.get('/scores/ranking', { params: { period } })
}

export const meApi = {
  studyReport: () => http.get('/me/study-report')
}

export const nearbyApi = {
  nearest: (params) => http.get('/rooms/nearest-available', { params }),
  alternatives: (params) => http.get('/rooms/alternatives', { params })
}

export const aiApi = {
  assistant: (data) => http.post('/ai/assistant', data)
}

export const notificationApi = {
  list: () => http.get('/notifications'),
  unread: () => http.get('/notifications/unread-count'),
  read: (id) => http.post(`/notifications/${id}/read`),
  readAll: () => http.post('/notifications/read-all')
}

export const waitlistApi = {
  join: (data) => http.post('/waitlist', data),
  mine: () => http.get('/waitlist/me'),
  accept: (id) => http.post(`/waitlist/${id}/accept`),
  cancel: (id) => http.post(`/waitlist/${id}/cancel`)
}

export const announcementApi = {
  list: () => http.get('/announcements'),
  adminList: () => http.get('/admin/announcements'),
  create: (data) => http.post('/admin/announcements', data),
  update: (id, data) => http.put(`/admin/announcements/${id}`, data),
  remove: (id) => http.delete(`/admin/announcements/${id}`)
}

export const blacklistApi = {
  me: () => http.get('/blacklist/me'),
  list: () => http.get('/admin/blacklist'),
  release: (id) => http.post(`/admin/blacklist/${id}/release`)
}
