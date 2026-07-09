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
    <div class="seat-grid" :style="{ gridTemplateColumns: `repeat(${cols}, 40px)` }">
      <div v-for="cell in orderedCells" :key="cell.seatId ?? (cell.rowIndex + '-' + cell.colIndex)"
           class="seat-cell"
           :class="cellClass(cell)"
           @click="onClick(cell)">
        <span v-if="effStatus(cell) === 'HELD'">🔒{{ remain(cell) }}</span>
        <span v-else-if="cell.cellType === 'SEAT'">{{ shortNo(cell.seatNo) }}</span>
        <span v-else-if="cell.cellType === 'AISLE'">·</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  cells: { type: Array, default: () => [] },
  cols: { type: Number, default: 8 },
  selectable: { type: Boolean, default: false },
  selectedSeatId: { type: [Number, String], default: null },
  selectedIds: { type: Array, default: () => [] },
  nowMs: { type: Number, default: 0 }
})
const emit = defineEmits(['select'])

const orderedCells = computed(() =>
  [...props.cells].sort((a, b) => (a.rowIndex - b.rowIndex) || (a.colIndex - b.colIndex))
)

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
