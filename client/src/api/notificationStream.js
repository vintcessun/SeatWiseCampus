// 每用户通知 SSE 订阅（token 走查询参数）
export function connectNotifications(handlers = {}) {
  const token = localStorage.getItem('satoken') || ''
  let es = null, closed = false, retry = 0
  const open = () => {
    es = new EventSource(`/api/notifications/stream?token=${encodeURIComponent(token)}`)
    es.addEventListener('notification', (e) => {
      try { handlers.notification && handlers.notification(JSON.parse(e.data)) } catch {}
    })
    es.onerror = () => {
      es.close()
      if (closed) return
      retry = Math.min(retry + 1, 6)
      setTimeout(() => { if (!closed) open() }, retry * 1000)
    }
  }
  open()
  return { close() { closed = true; if (es) es.close() } }
}
