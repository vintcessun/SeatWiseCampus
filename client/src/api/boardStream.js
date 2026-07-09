/**
 * SSE 看板订阅封装。EventSource 不能自定义请求头，token 通过查询参数传递。
 */
export function connectBoardStream({ roomId, date }, handlers = {}) {
  const token = localStorage.getItem('satoken') || ''
  const url = `/api/board/stream?roomId=${roomId}&date=${date}&token=${encodeURIComponent(token)}`
  let es = null
  let closed = false
  let retry = 0

  const open = () => {
    es = new EventSource(url)
    const bind = (name) => {
      es.addEventListener(name, (e) => {
        retry = 0
        try {
          handlers[name] && handlers[name](JSON.parse(e.data))
        } catch (err) {
          handlers[name] && handlers[name](e.data)
        }
      })
    }
    ;['board_snapshot', 'seat_reserved', 'seat_released', 'seat_in_use', 'seat_disabled', 'seat_hold', 'hold_released', 'heartbeat'].forEach(bind)

    es.onopen = () => { handlers.onOpen && handlers.onOpen() }
    es.onerror = () => {
      es.close()
      if (closed) return
      handlers.onError && handlers.onError()
      retry = Math.min(retry + 1, 6)
      setTimeout(() => { if (!closed) open() }, retry * 1000)
    }
  }
  open()

  return {
    close() {
      closed = true
      if (es) es.close()
    }
  }
}
