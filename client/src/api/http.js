import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截：注入 token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('satoken')
  if (token) {
    config.headers['satoken'] = token
  }
  return config
})

// 响应拦截：统一解包与错误处理
http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && (body.code === '0' || body.code === 0)) {
      return body.data
    }
    const code = body?.code
    const message = body?.message || '请求失败'
    handleBizError(code, message)
    return Promise.reject({ code, message })
  },
  (error) => {
    const status = error?.response?.status
    const body = error?.response?.data
    const code = body?.code || String(status)
    const message = body?.message || '网络异常，请稍后重试'
    handleBizError(code, message)
    return Promise.reject({ code, message })
  }
)

function handleBizError(code, message) {
  if (code === 'AUTH_REQUIRED' || code === '401') {
    localStorage.removeItem('satoken')
    localStorage.removeItem('role')
    if (router.currentRoute.value.path !== '/login') {
      ElMessage.warning('登录已过期，请重新登录')
      router.push('/login')
    }
    return
  }
  ElMessage.error(message)
}

export default http
