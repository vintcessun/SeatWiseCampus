<template>
  <span>{{ display }}</span>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'

const props = defineProps({
  value: { type: Number, default: 0 },
  duration: { type: Number, default: 900 },
  decimals: { type: Number, default: 0 }
})

const display = ref('0')
let raf = null

function run(to) {
  const from = 0
  const start = performance.now()
  cancelAnimationFrame(raf)
  const tick = (now) => {
    const t = Math.min(1, (now - start) / props.duration)
    const eased = 1 - Math.pow(1 - t, 3)        // easeOutCubic
    const v = from + (to - from) * eased
    display.value = v.toFixed(props.decimals)
    if (t < 1) raf = requestAnimationFrame(tick)
    else display.value = to.toFixed(props.decimals)
  }
  raf = requestAnimationFrame(tick)
}

onMounted(() => run(props.value || 0))
watch(() => props.value, (v) => run(v || 0))
</script>
