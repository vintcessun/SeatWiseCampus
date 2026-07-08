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
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <el-card shadow="never" style="margin-bottom:16px">
      <div style="display:flex;gap:12px;align-items:center">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="reload" />
        <el-time-select v-model="start" start="08:00" end="21:30" step="00:30" style="width:120px" @change="reload" />
        <el-time-select v-model="end" start="08:30" end="22:00" step="00:30" style="width:120px" @change="reload" />
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="18">
        <el-card shadow="never">
          <SeatGrid :cells="board.seats || []" :cols="board.cols || 8" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-card"><div class="num" style="color:#1f9d55">{{ counts.FREE }}</div><div class="lbl">空闲</div></div>
          <el-divider />
          <div class="stat-card"><div class="num" style="color:#d98a00">{{ counts.RESERVED }}</div><div class="lbl">已预约</div></div>
          <el-divider />
          <div class="stat-card"><div class="num" style="color:#d64545">{{ counts.USING }}</div><div class="lbl">使用中</div></div>
          <el-divider />
          <div class="stat-card"><div class="num" style="color:#a2abbd">{{ counts.DISABLED }}</div><div class="lbl">不可用</div></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
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
let stream = null

const counts = computed(() => {
  const c = { FREE: 0, RESERVED: 0, USING: 0, DISABLED: 0 }
  for (const s of board.seats || []) if (c[s.status] !== undefined) c[s.status]++
  return c
})

onMounted(() => { reload(); openStream() })
onBeforeUnmount(() => { if (stream) stream.close() })

async function reload() {
  Object.assign(board, await boardApi.snapshot(roomId, { date: date.value, start: start.value, end: end.value }))
}
function openStream() {
  if (stream) stream.close()
  stream = connectBoardStream({ roomId, date: date.value }, {
    onOpen: () => (sseOk.value = true),
    onError: () => (sseOk.value = false),
    seat_reserved: (p) => apply(p, 'RESERVED'),
    seat_released: (p) => apply(p, 'FREE'),
    seat_in_use: (p) => apply(p, 'USING'),
    seat_disabled: (p) => apply(p, 'DISABLED')
  })
}
function apply(p, status) {
  const s = (board.seats || []).find(x => x.seatId === p.seatId)
  if (s) s.status = status
}
</script>
