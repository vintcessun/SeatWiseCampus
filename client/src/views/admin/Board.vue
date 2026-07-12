<template>
  <div class="page">
    <div style="display:flex;justify-content:space-between;align-items:center">
      <div>
        <div class="page-title">{{ board.roomName || '实时看板' }}</div>
        <div class="page-sub">
          <el-tag size="small" :type="sseOk ? 'success' : 'info'" effect="plain">{{ sseOk ? '实时连接中' : '连接中…' }}</el-tag>
          多客户端座位状态秒级同步
        </div>
      </div>
      <div style="display:flex;gap:8px">
        <el-button type="primary" plain :icon="VideoPlay" @click="$router.push(`/admin/rooms/${roomId}/replay`)">历史回放</el-button>
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </div>

    <el-card shadow="never" style="margin-bottom:16px">
      <div style="display:flex;gap:12px;align-items:center">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="reload" />
        <el-time-select v-model="start" start="08:00" end="21:30" step="00:30" style="width:120px" @change="reload" />
        <el-time-select v-model="end" start="08:30" end="22:00" step="00:30" style="width:120px" @change="reload" />
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="17">
        <el-card shadow="never">
          <!-- GateGuard: SeatGrid now needs :rows for edge overlay positioning. board carries rows from BoardVO. -->
          <SeatGrid :cells="board.seats || []" :cols="board.cols || 8" :rows="board.rows || 6" :now-ms="nowMs" :selected-seat-id="flashId" :features="board.features" />
        </el-card>
      </el-col>
      <el-col :span="7">
        <el-card shadow="never" style="margin-bottom:16px">
          <div class="stat-row">
            <div class="mini"><div class="num" style="color:#1f9d55">{{ counts.FREE }}</div><div class="lbl">空闲</div></div>
            <div class="mini"><div class="num" style="color:#b8860b">{{ counts.HELD }}</div><div class="lbl">选择中</div></div>
            <div class="mini"><div class="num" style="color:#d98a00">{{ counts.RESERVED }}</div><div class="lbl">已预约</div></div>
            <div class="mini"><div class="num" style="color:#d64545">{{ counts.USING }}</div><div class="lbl">使用中</div></div>
            <div class="mini"><div class="num" style="color:#a2abbd">{{ counts.DISABLED }}</div><div class="lbl">不可用</div></div>
          </div>
        </el-card>
        <el-card shadow="never">
          <div style="font-weight:700;margin-bottom:8px;display:flex;align-items:center;gap:6px">
            <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#1f9d55" :style="{opacity: sseOk?1:.3}"></span>
            实时事件流
          </div>
          <div class="feed">
            <div v-if="!events.length" style="color:#a2abbd;font-size:12px;text-align:center;padding:16px">等待实时事件…</div>
            <div v-for="(e, i) in events" :key="i" class="feed-item" @click="locate(e.seatId)">
              <span class="feed-time">{{ e.time }}</span>
              <span class="feed-dot" :style="{ background: e.color }"></span>
              <span class="feed-text">{{ e.text }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { VideoPlay } from '@element-plus/icons-vue'
import SeatGrid from '../../components/SeatGrid.vue'
import { boardApi } from '../../api'
import { connectBoardStream } from '../../api/boardStream'
import { todayLocal } from '../../utils/date'

const route = useRoute()
const roomId = route.params.roomId
const date = ref(todayLocal())
const start = ref('14:00')
const end = ref('16:00')
const board = reactive({ seats: [], cols: 8, roomName: '' })
const sseOk = ref(false)
const nowMs = ref(Date.now())
const events = ref([])
const flashId = ref(null)
let stream = null
let ticker = null

const counts = computed(() => {
  const c = { FREE: 0, HELD: 0, RESERVED: 0, USING: 0, DISABLED: 0 }
  for (const s of board.seats || []) if (c[s.status] !== undefined) c[s.status]++
  return c
})

function seatNo(seatId) { return (board.seats || []).find(s => s.seatId === seatId)?.seatNo || ('#' + seatId) }
function logEvent(seatId, text, color) {
  const d = new Date()
  const t = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
  events.value.unshift({ time: t, seatId, text: `${seatNo(seatId)} ${text}`, color })
  if (events.value.length > 40) events.value.pop()
}
function locate(seatId) {
  flashId.value = seatId
  setTimeout(() => { if (flashId.value === seatId) flashId.value = null }, 2000)
}

onMounted(() => { reload(); openStream(); ticker = setInterval(() => nowMs.value = Date.now(), 1000) })
onBeforeUnmount(() => { if (stream) stream.close(); if (ticker) clearInterval(ticker) })

async function reload() {
  Object.assign(board, await boardApi.snapshot(roomId, { date: date.value, start: start.value, end: end.value }))
}
function openStream() {
  if (stream) stream.close()
  stream = connectBoardStream({ roomId, date: date.value }, {
    onOpen: () => (sseOk.value = true),
    onError: () => (sseOk.value = false),
    seat_reserved: (p) => { set(p.seatId, { status: 'RESERVED', holdExpireAt: null }); logEvent(p.seatId, '被预约', '#d98a00') },
    seat_released: (p) => { set(p.seatId, { status: 'FREE', holdExpireAt: null }); logEvent(p.seatId, '释放', '#1f9d55') },
    seat_in_use: (p) => { set(p.seatId, { status: 'USING' }); logEvent(p.seatId, '完成签到', '#d64545') },
    seat_disabled: (p) => { set(p.seatId, { status: 'DISABLED' }); logEvent(p.seatId, '被禁用', '#a2abbd') },
    seat_hold: (p) => { set(p.seatId, { status: 'HELD', holdExpireAt: p.expireAt }); logEvent(p.seatId, '临时锁定', '#b8860b') },
    hold_released: (p) => { set(p.seatId, { status: 'FREE', holdExpireAt: null }); logEvent(p.seatId, '取消选择', '#1f9d55') }
  })
}
function set(seatId, patch) {
  const s = (board.seats || []).find(x => x.seatId === seatId)
  if (s) Object.assign(s, patch)
}
</script>

<style scoped>
.stat-row { display: flex; justify-content: space-between; text-align: center; }
.mini .num { font-size: 22px; font-weight: 800; }
.mini .lbl { font-size: 11px; color: #8a93a6; }
.feed { max-height: 460px; overflow-y: auto; }
.feed-item { display: flex; align-items: center; gap: 8px; padding: 6px 4px; border-bottom: 1px dashed #eef0f5; font-size: 12px; cursor: pointer; }
.feed-item:hover { background: #f7f8fc; }
.feed-time { color: #a2abbd; font-variant-numeric: tabular-nums; }
.feed-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.feed-text { color: #3b3f4a; }
</style>
