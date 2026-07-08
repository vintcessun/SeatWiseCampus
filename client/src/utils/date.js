// 返回本地时区的今天 YYYY-MM-DD（避免 toISOString 的 UTC 偏移在 +8 区跨天）
export function todayLocal() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
