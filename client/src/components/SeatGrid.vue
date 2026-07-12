<template>
  <div>
    <div class="legend">
      <div class="legend-item"><span class="legend-dot seat-FREE"></span>空闲</div>
      <div class="legend-item"><span class="legend-dot seat-HELD"></span>选择中</div>
      <div class="legend-item"><span class="legend-dot seat-RESERVED"></span>已预约</div>
      <div class="legend-item"><span class="legend-dot seat-USING"></span>使用中</div>
      <div class="legend-item"><span class="legend-dot seat-DISABLED"></span>不可用</div>
      <div class="legend-item"><span class="legend-dot" style="background:#fff;outline:3px solid #3b6cff"></span>我的预约</div>
    </div>

    <div class="unified-grid" :style="gridStyle">
      <!-- 外圈：边缘标记（门/讲台/角落留白），始终显示 -->
      <div v-for="ec in edgeCells" :key="ec.key"
        class="edge-cell" :class="{ placed: ec.placed }"
        :style="{ gridRow: ec.row, gridColumn: ec.col }">
        {{ ec.label }}
      </div>
      <!-- 内圈：座位网格 -->
      <div v-for="cell in orderedCells" :key="cell.seatId ?? (cell.rowIndex + '-' + cell.colIndex)"
        class="seat-cell" :class="cellClass(cell)"
        :style="{ gridRow: cell.rowIndex + 2, gridColumn: cell.colIndex + 2 }"
        @click="onClick(cell)">
        <span v-if="effStatus(cell) === 'HELD'">🔒{{ remain(cell) }}</span>
        <span v-else-if="cell.cellType === 'SEAT'">{{ shortNo(cell.seatNo) }}</span>
        <span v-else-if="cell.cellType === 'AISLE'">·</span>
        <div v-if="cell.cellType === 'SEAT' && toTagKeys(cell.tags).length" class="seat-tags">
          <span v-for="k in toTagKeys(cell.tags)" :key="k" class="seat-tag-badge">{{ tagShort(k) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { toTagKeys, tagShort } from '../constants/seatTags'

const props = defineProps({
  cells: { type: Array, default: () => [] },
  cols: { type: Number, default: 8 },
  rows: { type: Number, default: 6 },
  selectable: { type: Boolean, default: false },
  selectedSeatId: { type: [Number, String], default: null },
  selectedIds: { type: Array, default: () => [] },
  nowMs: { type: Number, default: 0 },
  features: { type: [String, Object], default: null }
})
const emit = defineEmits(['select'])

const orderedCells = computed(() =>
  [...props.cells].sort((a, b) => (a.rowIndex - b.rowIndex) || (a.colIndex - b.colIndex))
)

// 统一网格：(rows+2) × (cols+2)，外圈为边缘标记，内圈为座位
const gridStyle = computed(() => ({
  gridTemplateColumns: `28px repeat(${props.cols}, 40px) 28px`,
  gridTemplateRows: `40px repeat(${props.rows}, 40px) 40px`,
  gap: '8px'
}))

// 边缘标记（门/讲台）- 完全在网格外部渲染，不占用任何 seat-cell
function parseFeatures() {
  const f = props.features
  if (!f) return []
  const raw = typeof f === 'object' ? f : (() => { try { return JSON.parse(f) } catch { return {} } })()
  const conv = (list, type) => (list || []).map(item => {
    if (item.edge) return { edge: item.edge, offset: item.offset ?? 0, type }
    const r = item.row ?? 0; const c = item.col ?? 0
    const rr = props.rows || 6; const cc = props.cols || 8
    if (r === 0) return { edge: 'top', offset: c, type }
    if (r === rr - 1) return { edge: 'bottom', offset: c, type }
    if (c === 0) return { edge: 'left', offset: r, type }
    return { edge: 'right', offset: r, type }
  })
  return [...conv(raw.doors, 'DOOR'), ...conv(raw.podiums, 'PODIUM')]
}
const allEdges = computed(() => parseFeatures())

function edgeType(edge, offset) {
  const f = allEdges.value.find(e => e.edge === edge && e.offset === offset)
  return f ? f.type : null
}

// 外圈单元格：(rows+2)×(cols+2) 网格的边界格子
const edgeCells = computed(() => {
  const rCount = props.rows, cCount = props.cols
  const cells = []
  // 4个角落
  cells.push({ key: 'corner-tl', row: 1, col: 1, label: '', placed: false })
  cells.push({ key: 'corner-tr', row: 1, col: cCount + 2, label: '', placed: false })
  cells.push({ key: 'corner-bl', row: rCount + 2, col: 1, label: '', placed: false })
  cells.push({ key: 'corner-br', row: rCount + 2, col: cCount + 2, label: '', placed: false })
  // 顶部边缘
  for (let c = 0; c < cCount; c++) {
    const t = edgeType('top', c)
    cells.push({ key: 'et' + c, row: 1, col: c + 2, label: t === 'DOOR' ? '🚪' : t === 'PODIUM' ? '讲' : '', placed: !!t })
  }
  // 底部边缘
  for (let c = 0; c < cCount; c++) {
    const t = edgeType('bottom', c)
    cells.push({ key: 'eb' + c, row: rCount + 2, col: c + 2, label: t === 'DOOR' ? '🚪' : t === 'PODIUM' ? '讲' : '', placed: !!t })
  }
  // 左侧边缘
  for (let r = 0; r < rCount; r++) {
    const t = edgeType('left', r)
    cells.push({ key: 'el' + r, row: r + 2, col: 1, label: t === 'DOOR' ? '🚪' : t === 'PODIUM' ? '讲' : '', placed: !!t })
  }
  // 右侧边缘
  for (let r = 0; r < rCount; r++) {
    const t = edgeType('right', r)
    cells.push({ key: 'er' + r, row: r + 2, col: cCount + 2, label: t === 'DOOR' ? '🚪' : t === 'PODIUM' ? '讲' : '', placed: !!t })
  }
  return cells
})

// 锁座到期后本地视为空闲
function effStatus(cell) {
  if (cell.status === 'HELD') {
    const now = props.nowMs || Date.now()
    if (cell.holdExpireAt && cell.holdExpireAt <= now) return 'FREE'
    return 'HELD'
  }
  return cell.status
}
function remain(cell) {
  const now = props.nowMs || Date.now()
  return Math.max(0, Math.ceil(((cell.holdExpireAt || now) - now) / 1000))
}

function cellClass(cell) {
  const st = effStatus(cell) || cell.cellType
  const cls = ['seat-' + st]
  if (cell.mine && st === 'HELD') cls.push('seat-mine-hold')
  else if (cell.mine) cls.push('seat-mine')
  if (props.selectedSeatId && cell.seatId === props.selectedSeatId) cls.push('seat-selected')
  if (props.selectedIds && props.selectedIds.includes(cell.seatId)) cls.push('seat-selected')
  if (props.selectable && cell.cellType === 'SEAT' && st === 'FREE') cls.push('clickable')
  return cls
}

function onClick(cell) {
  if (props.selectable && cell.cellType === 'SEAT' && effStatus(cell) === 'FREE') {
    emit('select', cell)
  }
}

function shortNo(no) {
  if (!no) return ''
  const parts = no.split('-')
  return parts.length > 1 ? parts[0] + parts[1] : no
}
</script>

<style scoped>
/* 统一网格：(rows+2)×(cols+2) */
.unified-grid { display: inline-grid; }

/* 边缘单元格：始终可见，有标记时着色 */
.edge-cell {
  display: grid; place-items: center;
  font-size: 12px; user-select: none;
  border-radius: 6px;
  background: #f8f6f0; border: 1px dashed #ddd;
}
.edge-cell.placed {
  background: #f5f0e8; border: 1px solid #e0d5c0; color: #8b6914;
  border-style: solid;
}
</style>
