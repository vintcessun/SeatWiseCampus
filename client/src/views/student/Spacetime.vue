<template>
  <div class="page">
    <div class="page-title">🛰️ 时空座位图</div>
    <div class="page-sub">拖动时间轴选择<strong>预约开始时刻</strong>，座位按「从该时刻起的连续可用时长」着色发光——一眼找到既现在空闲、又能坐得久的位置。</div>

    <el-card shadow="never" style="margin-bottom:16px">
      <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
        <el-select v-model="roomId" style="width:200px" @change="load">
          <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
        </el-select>
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="load" />
        <el-button :type="playing ? 'warning' : 'primary'" :icon="playing ? VideoPause : VideoPlay" @click="togglePlay">
          {{ playing ? '暂停' : '播放一天' }}
        </el-button>
        <div style="margin-left:auto;font-size:13px;color:#8a93a6">此刻空闲 <b style="color:#1f9d55">{{ freeCount }}</b> / {{ data.totalSeats || 0 }} · 可预约到 <b>{{ current?.label }}</b> 起</div>
      </div>
      <div style="padding:6px 6px 0">
        <div style="font-weight:700;font-size:26px;color:var(--el-color-primary);letter-spacing:1px">{{ current?.label || '--:--' }}</div>
        <el-slider v-model="fi" :min="0" :max="Math.max(0, frames.length - 1)"
          :format-tooltip="i => frames[i]?.label || ''" :marks="marks" @input="pause" />
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="17">
        <el-card shadow="never">
          <div class="heat-legend">
            连续可用时长：
            <span class="hl"><i style="background:#e6f6ec"></i>&lt;30m</span>
            <span class="hl"><i style="background:#9be0b4"></i>1h</span>
            <span class="hl"><i style="background:#34c17b"></i>2h</span>
            <span class="hl"><i style="background:#12915a;box-shadow:0 0 8px #12915a"></i>≥3h</span>
            <span class="hl"><i style="background:#ffd9d9"></i>占用</span>
          </div>
          <div class="stgrid" :style="{ gridTemplateColumns: `repeat(${data.cols || 8}, 46px)` }">
            <div v-for="c in orderedCells" :key="c.seatId ?? (c.rowIndex + '-' + c.colIndex)"
              class="stcell" :class="cellClass(c)" :style="cellStyle(c)"
              @click="c.cellType === 'SEAT' && c.enabled && !c.occupied ? pick(c) : null"
              :title="c.cellType === 'SEAT' ? c.seatNo + (c.occupied ? ' · 占用中' : ' · 连续可用 ' + c.freeHours + 'h') : ''">
              <span v-if="c.cellType === 'SEAT' && !c.occupied && c.freeHours >= 1" class="hh">{{ c.freeHours }}h</span>
              <span v-else-if="c.cellType === 'SEAT'">{{ shortNo(c.seatNo) }}</span>
              <span v-else-if="c.cellType === 'AISLE'" style="opacity:.4">·</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="7">
        <el-card shadow="never">
          <div style="font-weight:700;margin-bottom:8px">📈 全天占用曲线</div>
          <div class="spark">
            <div v-for="(f, i) in frames" :key="f.slotIndex" class="bar" :class="{ on: i === fi }"
              :style="{ height: barH(f) }" :title="`${f.label} · ${f.occupiedCount} 占用`" @click="fi = i; pause()"></div>
          </div>
          <el-alert type="info" :closable="false" show-icon style="margin-top:14px"
            title="点击发光的绿色座位，即可带着「该开始时刻」跳转到选座页一键预约。" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dlg" :title="picked ? ('座位 ' + picked.seatNo + ' · 当日时段') : ''" width="460px">
      <div v-if="picked">
        <div class="tl">
          <div v-for="f in frames" :key="f.slotIndex" class="tlc"
            :class="{ occ: occ[picked.seatId]?.[f._i], sel: f._i === fi }"
            :title="f.label + (occ[picked.seatId]?.[f._i] ? ' 占用' : ' 空闲')"></div>
        </div>
        <div style="display:flex;justify-content:space-between;font-size:12px;color:#8a93a6;margin-top:4px">
          <span>{{ frames[0]?.label }}</span><span>{{ frames[frames.length - 1]?.label }}</span>
        </div>
        <p style="margin:14px 0 0">从 <b>{{ current?.label }}</b> 起连续可用 <b style="color:#1f9d55">{{ picked.freeHours }} 小时</b>。</p>
      </div>
      <template #footer>
        <el-button @click="dlg = false">关闭</el-button>
        <el-button type="primary" @click="goBook">以 {{ current?.label }} 为开始去预约 →</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { VideoPlay, VideoPause } from '@element-plus/icons-vue'
import { boardApi, baseApi } from '../../api'
import { todayLocal } from '../../utils/date'

const router = useRouter()
const rooms = ref([])
const roomId = ref(null)
const date = ref(todayLocal())
const data = reactive({ cols: 8, totalSeats: 0, seats: [], timeline: [] })
const fi = ref(0)
const playing = ref(false)
const occ = reactive({})       // seatId -> bool[] over timeline
let timer = null

const SLOT_MIN = 30
const frames = computed(() => data.timeline || [])
const current = computed(() => frames.value[fi.value])

const orderedCells = computed(() => {
  const oc = occ
  const idx = fi.value
  return [...(data.seats || [])]
    .sort((a, b) => (a.rowIndex - b.rowIndex) || (a.colIndex - b.colIndex))
    .map(s => {
      if (s.cellType !== 'SEAT') return { ...s, occupied: false, freeHours: 0 }
      const occupied = !!(oc[s.seatId] && oc[s.seatId][idx])
      let free = 0
      if (!occupied && oc[s.seatId]) { for (let j = idx; j < oc[s.seatId].length && !oc[s.seatId][j]; j++) free++ }
      return { ...s, occupied, freeHours: Math.round(free * SLOT_MIN / 60 * 10) / 10 }
    })
})
const freeCount = computed(() => orderedCells.value.filter(c => c.cellType === 'SEAT' && c.enabled && !c.occupied).length)

function cellClass(c) {
  if (c.cellType !== 'SEAT') return ['aisle']
  if (!c.enabled) return ['disabled']
  return c.occupied ? ['occ'] : ['free', 'clickable']
}
function cellStyle(c) {
  if (c.cellType !== 'SEAT' || !c.enabled || c.occupied) return {}
  const t = Math.min(1, c.freeHours / 3)   // 0=浅绿 … 1=深绿
  // 浅绿 rgb(230,246,236) → 深绿 rgb(18,145,90)
  const r = Math.round(230 - t * 212)
  const g = Math.round(246 - t * 101)
  const b = Math.round(236 - t * 146)
  const glow = c.freeHours >= 3 ? '0 0 12px rgba(18,145,90,.6)' : c.freeHours >= 2 ? '0 0 7px rgba(52,193,123,.45)' : 'none'
  return { background: `rgb(${r},${g},${b})`, color: t > 0.55 ? '#fff' : '#177245', boxShadow: glow }
}
function shortNo(no) { if (!no) return ''; const p = no.split('-'); return p.length > 1 ? p[0] + p[1] : no }

const maxOcc = computed(() => Math.max(1, ...frames.value.map(f => f.occupiedCount)))
function barH(f) { return 8 + Math.round(f.occupiedCount / maxOcc.value * 46) + 'px' }
const marks = computed(() => { const m = {}; frames.value.forEach((f, i) => { if (f.label.endsWith(':00') && f.slotIndex % 4 === 0) m[i] = f.label }); return m })

const dlg = ref(false)
const picked = ref(null)
function pick(c) { picked.value = c; dlg.value = true }
function goBook() {
  router.push({ path: `/student/rooms/${roomId.value}/seats`, query: { date: date.value, start: current.value.label } })
}

function pause() { playing.value = false }
function togglePlay() { playing.value ? pause() : (playing.value = true) }
watch(playing, () => {
  if (timer) clearInterval(timer)
  if (!playing.value) return
  timer = setInterval(() => { if (fi.value >= frames.value.length - 1) { pause(); return } fi.value++ }, 550)
})

async function load() {
  pause()
  if (!roomId.value) return
  const d = await boardApi.replay(roomId.value, date.value)
  Object.assign(data, d)
  // 预构建占用矩阵
  Object.keys(occ).forEach(k => delete occ[k])
  const tl = data.timeline || []
  tl.forEach((f, i) => { f._i = i })
  ;(data.seats || []).forEach(s => { if (s.cellType === 'SEAT') occ[s.seatId] = tl.map(() => false) })
  tl.forEach((f, i) => { (f.occupied || []).forEach(sid => { if (occ[sid]) occ[sid][i] = true }) })
  fi.value = 0
}

onMounted(async () => {
  try {
    const campuses = await baseApi.campuses()
    const cid = campuses[0]?.id
    rooms.value = await baseApi.rooms({ campusId: cid })
    roomId.value = rooms.value[0]?.id
    await load()
  } catch (e) { /* ignore */ }
})
onBeforeUnmount(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.heat-legend { display:flex; align-items:center; gap:14px; font-size:12px; color:#8a93a6; margin-bottom:12px; flex-wrap:wrap; }
.heat-legend .hl { display:flex; align-items:center; gap:5px; }
.heat-legend .hl i { width:14px; height:14px; border-radius:4px; display:inline-block; }
.stgrid { display:inline-grid; gap:8px; }
.stcell { width:46px; height:46px; border-radius:9px; display:grid; place-items:center; font-size:11px; font-weight:700;
  border:1px solid rgba(0,0,0,.05); transition:transform .12s ease, box-shadow .2s ease; }
.stcell.clickable { cursor:pointer; }
.stcell.clickable:hover { transform:translateY(-3px) scale(1.08); }
.stcell.occ { background:#ffe0e0; color:#d06; opacity:.55; }
.stcell.disabled { background:#e9ecf2; color:#a2abbd; }
.stcell.aisle { background:transparent; border:none; }
.stcell .hh { font-size:11px; }
.spark { display:flex; align-items:flex-end; gap:2px; height:60px; }
.spark .bar { flex:1; background:#c9d6ff; border-radius:2px 2px 0 0; cursor:pointer; min-width:2px; }
.spark .bar.on { background:#3b6cff; }
.tl { display:flex; gap:2px; }
.tl .tlc { flex:1; height:26px; border-radius:3px; background:#dff3e6; }
.tl .tlc.occ { background:#ffd0d0; }
.tl .tlc.sel { outline:2px solid #3b6cff; }
:global(html.dark) .stcell.occ { background:#3d1f24; color:#ff9aa8; }
:global(html.dark) .stcell.disabled { background:#232a3a; color:#6b7488; }
:global(html.dark) .tl .tlc { background:#16362a; }
:global(html.dark) .tl .tlc.occ { background:#3d1f24; }
</style>
