<template>
  <div>
    <div class="legend">
      <div class="legend-item"><span class="legend-dot seat-FREE"></span>空闲</div>
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
        <span v-if="cell.cellType === 'SEAT'">{{ shortNo(cell.seatNo) }}</span>
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
  selectedSeatId: { type: [Number, String], default: null }
})
const emit = defineEmits(['select'])

const orderedCells = computed(() =>
  [...props.cells].sort((a, b) => (a.rowIndex - b.rowIndex) || (a.colIndex - b.colIndex))
)

function cellClass(cell) {
  const cls = ['seat-' + (cell.status || cell.cellType)]
  if (cell.mine) cls.push('seat-mine')
  if (props.selectedSeatId && cell.seatId === props.selectedSeatId) cls.push('seat-selected')
  if (props.selectable && cell.cellType === 'SEAT' && cell.status === 'FREE') cls.push('clickable')
  return cls
}

function onClick(cell) {
  if (props.selectable && cell.cellType === 'SEAT' && cell.status === 'FREE') {
    emit('select', cell)
  }
}

function shortNo(no) {
  if (!no) return ''
  const parts = no.split('-')
  return parts.length > 1 ? parts[0] + parts[1] : no
}
</script>
