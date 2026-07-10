<template>
  <div class="page">
    <div class="page-title">🍅 专注番茄钟</div>
    <div class="page-sub">在自习时用番茄工作法保持专注：25 分钟专注 + 5 分钟短休，每 4 个番茄享受一次长休。</div>

    <div class="pomo-wrap">
      <div class="ring" :class="mode" :style="ringStyle">
        <div class="ring-inner">
          <div class="mode-tag">{{ modeLabel }}</div>
          <div class="time">{{ mm }}:{{ ss }}</div>
          <div class="sub">{{ running ? '专注中…' : '准备开始' }}</div>
        </div>
      </div>

      <div class="controls">
        <el-button size="large" type="primary" round :icon="running ? VideoPause : VideoPlay" @click="toggle">
          {{ running ? '暂停' : '开始' }}
        </el-button>
        <el-button size="large" round :icon="RefreshLeft" @click="reset">重置</el-button>
        <el-button size="large" round :icon="Right" @click="skip">跳过</el-button>
      </div>

      <div class="modes">
        <el-segmented v-model="mode" :options="modeOptions" @change="onModeChange" />
      </div>

      <div class="stats">
        <div class="stat"><div class="v">🍅 {{ todayCount }}</div><div class="k">今日完成番茄</div></div>
        <div class="stat"><div class="v">{{ round }}</div><div class="k">本轮进度 / 4</div></div>
        <div class="stat"><div class="v">{{ focusMin }}<small> 分</small></div><div class="k">今日专注时长</div></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount, watch } from 'vue'
import { VideoPlay, VideoPause, RefreshLeft, Right } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { burstConfetti } from '../../utils/confetti'

const DUR = { focus: 25 * 60, short: 5 * 60, long: 15 * 60 }
const modeOptions = [{ label: '专注 25', value: 'focus' }, { label: '短休 5', value: 'short' }, { label: '长休 15', value: 'long' }]
const modeLabelMap = { focus: '专注', short: '短休息', long: '长休息' }

const mode = ref('focus')
const remain = ref(DUR.focus)
const running = ref(false)
const round = ref(0)
let timer = null

const todayKey = 'sw-pomo-' + new Date().toISOString().slice(0, 10)
const todayCount = ref(Number(localStorage.getItem(todayKey) || 0))
const focusMin = computed(() => todayCount.value * 25)
const modeLabel = computed(() => modeLabelMap[mode.value])
const mm = computed(() => String(Math.floor(remain.value / 60)).padStart(2, '0'))
const ss = computed(() => String(remain.value % 60).padStart(2, '0'))

const ringStyle = computed(() => {
  const total = DUR[mode.value]
  const pct = ((total - remain.value) / total) * 100
  const color = mode.value === 'focus' ? '#ff5b5b' : '#1f9d55'
  return { background: `conic-gradient(${color} ${pct * 3.6}deg, var(--el-fill-color-light) 0deg)` }
})

function tick() {
  if (remain.value > 0) { remain.value--; return }
  complete()
}
function toggle() {
  running.value = !running.value
}
watch(running, (v) => {
  if (timer) clearInterval(timer)
  if (v) timer = setInterval(tick, 1000)
})
function reset() { running.value = false; remain.value = DUR[mode.value] }
function onModeChange() { running.value = false; remain.value = DUR[mode.value] }
function skip() { complete(true) }

function beep() {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)()
    const o = ctx.createOscillator(); const g = ctx.createGain()
    o.connect(g); g.connect(ctx.destination)
    o.type = 'sine'; o.frequency.value = 880
    g.gain.setValueAtTime(0.001, ctx.currentTime)
    g.gain.exponentialRampToValueAtTime(0.3, ctx.currentTime + 0.02)
    g.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.6)
    o.start(); o.stop(ctx.currentTime + 0.6)
  } catch (e) { /* ignore */ }
}

function complete(skipped = false) {
  running.value = false
  if (mode.value === 'focus') {
    if (!skipped) {
      todayCount.value++
      localStorage.setItem(todayKey, String(todayCount.value))
      burstConfetti({ count: 90 })
      beep()
      ElMessage.success('🍅 完成一个番茄！休息一下吧')
    }
    round.value = (round.value + 1) % 4
    mode.value = round.value === 0 ? 'long' : 'short'
  } else {
    if (!skipped) beep()
    mode.value = 'focus'
  }
  remain.value = DUR[mode.value]
}

onBeforeUnmount(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.pomo-wrap { display: flex; flex-direction: column; align-items: center; gap: 22px; padding: 20px 0; }
.ring { width: 280px; height: 280px; border-radius: 50%; display: grid; place-items: center; transition: background .3s; box-shadow: 0 12px 40px rgba(31,45,80,.12); }
.ring-inner { width: 234px; height: 234px; border-radius: 50%; background: var(--el-bg-color); display: grid; place-items: center; text-align: center; }
.mode-tag { font-size: 15px; color: var(--el-text-color-secondary); font-weight: 600; }
.time { font-size: 62px; font-weight: 800; line-height: 1.1; letter-spacing: 2px; font-variant-numeric: tabular-nums; }
.sub { font-size: 13px; color: var(--el-text-color-secondary); }
.controls { display: flex; gap: 12px; }
.modes { margin-top: 2px; }
.stats { display: flex; gap: 40px; margin-top: 6px; }
.stat { text-align: center; }
.stat .v { font-size: 26px; font-weight: 800; }
.stat .v small { font-size: 14px; }
.stat .k { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 2px; }
</style>
