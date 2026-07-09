<template>
  <div class="page">
    <div style="display:flex;justify-content:space-between;align-items:center">
      <div>
        <div class="page-title">座位排布编辑</div>
        <div class="page-sub">点击座位可启用/禁用（DISABLED 座位学生端不可预约）</div>
      </div>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <el-card shadow="never" style="margin-bottom:16px">
      <div style="display:flex;gap:10px;align-items:center;flex-wrap:wrap">
        <span style="font-size:13px;color:#5a6172">快速生成网格：</span>
        <el-input-number v-model="genRows" :min="1" :max="20" size="small" /> <span style="font-size:13px">行</span>
        <el-input-number v-model="genCols" :min="1" :max="20" size="small" /> <span style="font-size:13px">列</span>
        <span style="font-size:13px">过道列</span>
        <el-input-number v-model="genAisle" :min="0" :max="19" size="small" />
        <el-button type="primary" @click="generate">生成布局</el-button>
        <span style="font-size:12px;color:#c0392b">注意：会覆盖现有座位</span>
      </div>
    </el-card>

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
const genRows = ref(6)
const genCols = ref(8)
const genAisle = ref(3)

async function generate() {
  await baseApi.generateLayout(roomId, { rows: genRows.value, cols: genCols.value, aisleCol: genAisle.value })
  ElMessage.success('已生成 ' + genRows.value + '×' + genCols.value + ' 座位网格')
  await load()
}

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
