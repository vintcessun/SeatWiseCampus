import { ref, onMounted, onUnmounted } from 'vue'

// 响应式判断是否为移动端视口（默认断点 768px）
export function useMobile(breakpoint = 768) {
  const isMobile = ref(typeof window !== 'undefined' ? window.innerWidth <= breakpoint : false)
  const onResize = () => { isMobile.value = window.innerWidth <= breakpoint }
  onMounted(() => window.addEventListener('resize', onResize))
  onUnmounted(() => window.removeEventListener('resize', onResize))
  return { isMobile }
}
