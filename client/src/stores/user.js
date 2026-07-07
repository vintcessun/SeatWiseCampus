import { defineStore } from 'pinia'
import { authApi } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('satoken') || '',
    role: localStorage.getItem('role') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),
  getters: {
    isLogin: (s) => !!s.token,
    isAdmin: (s) => s.role === 'ADMIN',
    isStudent: (s) => s.role === 'STUDENT'
  },
  actions: {
    async login(payload) {
      const data = await authApi.login(payload)
      this.token = data.token
      this.role = data.role
      this.userInfo = data.userInfo
      localStorage.setItem('satoken', data.token)
      localStorage.setItem('role', data.role)
      localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
      return data
    },
    async refreshProfile() {
      const info = await authApi.me()
      this.userInfo = info
      localStorage.setItem('userInfo', JSON.stringify(info))
    },
    logout() {
      this.token = ''
      this.role = ''
      this.userInfo = null
      localStorage.removeItem('satoken')
      localStorage.removeItem('role')
      localStorage.removeItem('userInfo')
    }
  }
})
