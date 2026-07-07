<template>
  <div class="page">
    <div style="display:flex;justify-content:space-between;align-items:center">
      <div>
        <div class="page-title">座位排布编辑</div>
        <div class="page-sub">点击座位可启用/禁用（DISABLED 座位学生端不可预约）</div>
      </div>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <el-card shadow="never">
      <div class="seat-grid" :style="{ gridTemplateColumns: `repeat(${cols}, 40px)` }">
        <div v-for="cell in cells" :key="cell.rowIndex + '-' + cell.colIndex"
             class="seat-cell"
             :class="[cell.cellType==='SEAT' ? (cell.enabled ? 'seat-FREE clickable' : 'seat-DISABLED clickable') : 'seat-' + cell.cellType]"
             @click="toggle(cell)">
          <span v-if="cell.cellType==='SEAT'">{{ shortNo(cell.seatNo) }}</span>
          <span v-else-if="cell.cellType==='AISLE'">·</span>
        </div>
      </div>
      <div class="legend" style="margin-top:16px">
        <div class="legend-item"><span class="legend-dot seat-FREE"></span>启用座位</div>
        <div class="legend-item"><span class="legend-dot seat-DISABLED"></span>禁用座位</div>
        <div class="legend-item"><span class="legend-dot" style="background:transparent;border:1px dashed #ccc"></span>过道</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { baseApi } from '../../api'

const route = useRoute()
const roomId = route.params.roomId
const cells = ref([])
const cols = ref(8)

onMounted(load)
async function load() {
  const data = await baseApi.layout(roomId)
  cells.value = data.cells || []
  cols.value = data.cols || 8
}

async function toggle(cell) {
  if (cell.cellType !== 'SEAT') return
  const next = cell.enabled ? 0 : 1
  await baseApi.toggleSeat(cell.seatId, next)
  cell.enabled = next
  ElMessage.success(next ? '已启用 ' + cell.seatNo : '已禁用 ' + cell.seatNo)
}
function shortNo(no) {
  if (!no) return ''
  const p = no.split('-')
  return p.length > 1 ? p[0] + p[1] : no
}
</script>
