import http from './http'

export const authApi = {
  login: (data) => http.post('/auth/login', data),
  logout: () => http.post('/auth/logout'),
  me: () => http.get('/users/me')
}

export const baseApi = {
  campuses: () => http.get('/campuses'),
  buildings: (campusId) => http.get('/buildings', { params: { campusId } }),
  rooms: (params) => http.get('/study-rooms', { params }),
  layout: (roomId) => http.get(`/study-rooms/${roomId}/layout`),
  saveLayout: (roomId, data) => http.put(`/study-rooms/${roomId}/layout`, data),
  toggleSeat: (seatId, enabled) => http.post(`/seats/${seatId}/toggle`, null, { params: { enabled } }),
  createCampus: (data) => http.post('/campuses', data),
  createBuilding: (data) => http.post('/buildings', data),
  createRoom: (data) => http.post('/study-rooms', data)
}

export const boardApi = {
  snapshot: (roomId, params) => http.get(`/study-rooms/${roomId}/board`, { params })
}

export const reservationApi = {
  create: (data) => http.post('/reservations', data),
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

export const nearbyApi = {
  nearest: (params) => http.get('/rooms/nearest-available', { params })
}

export const blacklistApi = {
  me: () => http.get('/blacklist/me'),
  list: () => http.get('/admin/blacklist'),
  release: (id) => http.post(`/admin/blacklist/${id}/release`)
}
