<template>
  <div class="page">
    <div style="display:flex;justify-content:space-between;align-items:center">
      <div>
        <div class="page-title">🎬 座位历史回放 · {{ data.roomName }}</div>
        <div class="page-sub">拖动播放条重建当天座位占用轨迹，一眼看出「哪个时刻最拥挤」</div>
      </div>
      <div style="display:flex;gap:8px">
        <el-button :icon="Monitor" @click="$router.push(`/admin/rooms/${roomId}/board`)">实时看板</el-button>
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </div>

    <el-card shadow="never" style="margin-bottom:16px">
      <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="load" />
        <el-button :type="playing ? 'warning' : 'primary'" :icon="playing ? VideoPause : VideoPlay" @click="togglePlay">
          {{ playing ? '暂停' : '播放' }}
        </el-button>
        <el-button :icon="RefreshLeft" @click="frameIndex = 0">回到开场</el-button>
        <el-segmented v-model="speed" :options="speedOptions" />
        <div style="margin-left:auto;font-size:13px;color:#8a93a6">共 {{ frames.length }} 帧 · 每帧 {{ slotMinutes }} 分钟</div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="17">
        <el-card shadow="never">
          <!-- GateGuard: SeatGrid needs :rows. data.rows comes from ReplayVO. -->
          <SeatGrid :cells="cells" :cols="data.cols || 8" :rows="data.rows || 6" :features="data.features" />
          <div style="margin-top:16px;padding:0 6px">
            <el-slider v-model="frameIndex" :min="0" :max="Math.max(0, frames.length - 1)"
              :format-tooltip="i => frames[i]?.label || ''" :marks="marks" @input="pause" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="7">
        <el-card shadow="never" style="margin-bottom:16px;text-align:center">
          <div style="font-size:13px;color:#8a93a6">当前时刻</div>
          <div style="font-size:40px;font-weight:800;letter-spacing:1px;color:#3b6cff;line-height:1.2">{{ current?.label || '--:--' }}</div>
          <el-progress type="dashboard" :percentage="utilization" :color="utilColor" :width="130">
            <template #default>
              <div style="font-size:22px;font-weight:800">{{ utilization }}%</div>
              <div style="font-size:12px;color:#8a93a6">利用率</div>
            </template>
          </el-progress>
          <div style="margin-top:8px;font-size:14px">
            占用 <b style="color:#d98a00">{{ current?.occupiedCount || 0 }}</b> / {{ data.totalSeats || 0 }} 座
          </div>
        </el-card>
        <el-card shadow="never">
          <div style="font-weight:700;margin-bottom:10px">📈 全天占用曲线</div>
          <div class="spark">
            <div v-for="(f, i) in frames" :key="f.slotIndex"
              class="bar" :class="{ on: i === frameIndex, peak: f.slotIndex === peak.slotIndex }"
              :style="{ height: barH(f) }" :title="`${f.label} · ${f.occupiedCount} 座`"
              @click="jump(i)"></div>
          </div>
          <div style="margin-top:10px;font-size:13px;color:#8a93a6">
            🔥 最拥挤：<b style="color:#d64545">{{ peak.label }}</b>
            占用 {{ peak.occupiedCount }} 座（{{ peakUtil }}%）
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { VideoPlay, VideoPause, RefreshLeft, Monitor } from '@element-plus/icons-vue'
import SeatGrid from '../../components/SeatGrid.vue'
import { boardApi } from '../../api'
import { todayLocal } from '../../utils/date'

const route = useRoute()
const roomId = route.params.roomId
const date = ref(todayLocal())
const slotMinutes = 30
const data = reactive({ roomName: '', cols: 8, totalSeats: 0, seats: [], timeline: [] })
const frameIndex = ref(0)
const playing = ref(false)
const speed = ref('1x')
const speedOptions = ['0.5x', '1x', '2x']
let timer = null

const frames = computed(() => data.timeline || [])
const current = computed(() => frames.value[frameIndex.value])
const occupiedSet = computed(() => new Set(current.value?.occupied || []))

const cells = computed(() => (data.seats || []).map(s => {
  let status
  if (s.cellType !== 'SEAT') status = s.cellType
  else if (!s.enabled) status = 'DISABLED'
  else status = occupiedSet.value.has(s.seatId) ? 'RESERVED' : 'FREE'
  return { ...s, status }
}))

const utilization = computed(() => {
  const t = data.totalSeats || 0
  return t ? Math.round((current.value?.occupiedCount || 0) / t * 100) : 0
})
function utilColor(p) { return p >= 80 ? '#d64545' : p >= 50 ? '#d98a00' : '#1f9d55' }

const peak = computed(() => frames.value.reduce((a, b) => (b.occupiedCount > (a?.occupiedCount ?? -1) ? b : a), frames.value[0] || { label: '--:--', occupiedCount: 0, slotIndex: -1 }))
const peakUtil = computed(() => data.totalSeats ? Math.round(peak.value.occupiedCount / data.totalSeats * 100) : 0)
const maxOcc = computed(() => Math.max(1, ...frames.value.map(f => f.occupiedCount)))
function barH(f) { return 8 + Math.round(f.occupiedCount / maxOcc.value * 46) + 'px' }

const marks = computed(() => {
  const m = {}
  frames.value.forEach((f, i) => { if (f.label.endsWith(':00') && f.slotIndex % 4 === 0) m[i] = f.label })
  return m
})

function jump(i) { frameIndex.value = i; pause() }
function pause() { playing.value = false }
function togglePlay() { playing.value ? pause() : play() }
function play() {
  if (!frames.value.length) return
  if (frameIndex.value >= frames.value.length - 1) frameIndex.value = 0
  playing.value = true
}
watch([playing, speed], restartTimer)
function restartTimer() {
  if (timer) clearInterval(timer)
  if (!playing.value) return
  const ms = { '0.5x': 1000, '1x': 500, '2x': 250 }[speed.value] || 500
  timer = setInterval(() => {
    if (frameIndex.value >= frames.value.length - 1) { pause(); return }
    frameIndex.value++
  }, ms)
}

async function load() {
  pause()
  const d = await boardApi.replay(roomId, date.value)
  Object.assign(data, d)
  frameIndex.value = 0
}
onMounted(load)
onBeforeUnmount(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.spark { display:flex; align-items:flex-end; gap:2px; height:60px; }
.spark .bar { flex:1; background:#c9d6ff; border-radius:2px 2px 0 0; cursor:pointer; transition:background .15s; min-width:2px; }
.spark .bar:hover { background:#8fa8ff; }
.spark .bar.on { background:#3b6cff; }
.spark .bar.peak { background:#f0a0a0; }
.spark .bar.peak.on { background:#d64545; }
</style>
