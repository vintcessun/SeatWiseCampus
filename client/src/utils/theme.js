// 主题系统：明亮 / 深色，持久化到 localStorage，切换在 <html> 上加/去 .dark 类。
import { ref } from 'vue'

const KEY = 'sw-theme'
export const theme = ref(localStorage.getItem(KEY) || 'light')

function apply(t) {
  const el = document.documentElement
  if (t === 'dark') el.classList.add('dark')
  else el.classList.remove('dark')
}

export function initTheme() {
  apply(theme.value)
}

export function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem(KEY, theme.value)
  apply(theme.value)
}
