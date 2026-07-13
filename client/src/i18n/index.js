import { reactive, computed } from 'vue'
import zh from './locales/zh'
import en from './locales/en'

const messages = { zh, en }
const stored = localStorage.getItem('lang')
const state = reactive({ locale: messages[stored] ? stored : 'zh' })

function applyHtmlLang(l) {
  document.documentElement.lang = l === 'zh' ? 'zh-CN' : 'en'
}
applyHtmlLang(state.locale)

export function setLocale(l) {
  if (!messages[l]) return
  state.locale = l
  localStorage.setItem('lang', l)
  applyHtmlLang(l)
}

export function toggleLocale() {
  setLocale(state.locale === 'zh' ? 'en' : 'zh')
}

function get(obj, path) {
  return path.split('.').reduce((o, k) => (o == null ? undefined : o[k]), obj)
}

export function t(key, params) {
  let s = get(messages[state.locale], key)
  if (s == null) s = get(messages.zh, key)
  if (s == null) return key
  if (params) {
    for (const k in params) s = s.replace(new RegExp('\\{' + k + '\\}', 'g'), params[k])
  }
  return s
}

export const locale = computed(() => state.locale)

export default {
  install(app) {
    app.config.globalProperties.$t = t
    app.config.globalProperties.$locale = locale
  }
}
