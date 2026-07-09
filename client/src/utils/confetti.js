// 极简无依赖彩带：在页面顶层画布喷射一波彩色纸屑，用于预约/签到成功的庆祝反馈。
export function burstConfetti(opts = {}) {
  const count = opts.count || 130
  const colors = opts.colors || ['#3b6cff', '#8f5bff', '#1f9d55', '#ffb020', '#ff5b7f', '#22c1c3']
  const originX = opts.x ?? window.innerWidth / 2
  const originY = opts.y ?? window.innerHeight * 0.32

  const canvas = document.createElement('canvas')
  canvas.style.cssText = 'position:fixed;inset:0;width:100vw;height:100vh;pointer-events:none;z-index:99999'
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
  document.body.appendChild(canvas)
  const ctx = canvas.getContext('2d')

  const parts = Array.from({ length: count }, () => {
    const angle = Math.random() * Math.PI * 2
    const speed = 4 + Math.random() * 9
    return {
      x: originX, y: originY,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed - 6,
      size: 5 + Math.random() * 7,
      color: colors[(Math.random() * colors.length) | 0],
      rot: Math.random() * Math.PI,
      vr: (Math.random() - 0.5) * 0.4,
      life: 0,
      ttl: 70 + Math.random() * 40
    }
  })

  let raf
  function frame() {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    let alive = false
    for (const p of parts) {
      if (p.life > p.ttl) continue
      alive = true
      p.life++
      p.vy += 0.22            // 重力
      p.vx *= 0.99
      p.x += p.vx
      p.y += p.vy
      p.rot += p.vr
      const alpha = Math.max(0, 1 - p.life / p.ttl)
      ctx.save()
      ctx.globalAlpha = alpha
      ctx.translate(p.x, p.y)
      ctx.rotate(p.rot)
      ctx.fillStyle = p.color
      ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size * 0.6)
      ctx.restore()
    }
    if (alive) raf = requestAnimationFrame(frame)
    else { cancelAnimationFrame(raf); canvas.remove() }
  }
  frame()
}
