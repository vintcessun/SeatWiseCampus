<template>
  <div class="page">
    <div style="display:flex;justify-content:space-between;align-items:center">
      <div>
        <div class="page-title">{{ board.roomName || '选座' }}</div>
        <div class="page-sub">
          实时看板：
          <el-tag size="small" :type="sseOk ? 'success' : 'info'" effect="plain">
            {{ sseOk ? '实时连接中' : '连接中…' }}
          </el-tag>
        </div>
      </div>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <el-card shadow="never" style="margin-bottom:16px">
      <div style="display:flex;gap:12px;flex-wrap:wrap;align-items:center">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="reload" />
        <el-time-select v-model="start" start="08:00" end="21:30" step="00:30" placeholder="开始" style="width:130px" @change="reload" />
        <el-time-select v-model="end" start="08:30" end="22:00" step="00:30" placeholder="结束" style="width:130px" @change="reload" />
        <el-button :icon="Refresh" @click="reload">刷新座位</el-button>
        <span style="color:#8a93a6;font-size:13px">点击绿色空闲座位进行预约</span>
      </div>
    </el-card>

    <el-card shadow="never">
      <SeatGrid :cells="board.seats || []" :cols="board.cols || 8" selectable @select="onSelect" />
    </el-card>

    <el-dialog v-model="dialog" title="确认预约" width="380px">
      <p>自习室：{{ board.roomName }}</p>
      <p>座位：<b>{{ picked?.seatNo }}</b></p>
      <p>日期：{{ date }}</p>
      <p>时段：{{ start }} - {{ end }}</p>
      <template #footer>
        <el-button @click="dialog=false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确认预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import SeatGrid from '../../components/SeatGrid.vue'
import { boardApi, reservationApi } from '../../api'
import { connectBoardStream } from '../../api/boardStream'

const route = useRoute()
const roomId = route.params.roomId
const today = new Date().toISOString().slice(0, 10)
const date = ref(today)
const start = ref('14:00')
const end = ref('16:00')
const board = reactive({ seats: [], cols: 8, roomName: '' })
const dialog = ref(false)
const picked = ref(null)
const submitting = ref(false)
const sseOk = ref(false)
let stream = null

onMounted(() => { reload(); openStream() })
onBeforeUnmount(() => { if (stream) stream.close() })

async function reload() {
  const data = await boardApi.snapshot(roomId, { date: date.value, start: start.value, end: end.value })
  Object.assign(board, data)
}

function openStream() {
  if (stream) stream.close()
  stream = connectBoardStream({ roomId, date: date.value }, {
    onOpen: () => (sseOk.value = true),
    onError: () => (sseOk.value = false),
    seat_reserved: (p) => applyStatus(p, 'RESERVED'),
    seat_released: (p) => applyStatus(p, 'FREE'),
    seat_in_use: (p) => applyStatus(p, 'USING'),
    seat_disabled: (p) => applyStatus(p, 'DISABLED')
  })
}

function applyStatus(p, status) {
  const seat = (board.seats || []).find(s => s.seatId === p.seatId)
  if (seat) seat.status = status
}

function onSelect(cell) {
  picked.value = cell
  dialog.value = true
}

async function submit() {
  submitting.value = true
  try {
    await reservationApi.create({
      roomId: Number(roomId), seatId: picked.value.seatId,
      date: date.value, startTime: start.value, endTime: end.value
    })
    ElMessage.success('预约成功！座位已锁定，请按时签到')
    dialog.value = false
    await reload()
  } catch (e) {
    await reload() // 并发失败时刷新座位
  } finally {
    submitting.value = false
  }
}
</script>
