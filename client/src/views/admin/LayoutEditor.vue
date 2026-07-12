<template>
  <div class="page">
    <div style="display:flex;justify-content:space-between;align-items:center">
      <div>
        <div class="page-title">座位排布编辑</div>
        <div class="page-sub">支持手动逐格编辑（多过道 / 不规则房间）；存在未来预约时不可重排，避免影响已预约学生</div>
      </div>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <el-card shadow="never" style="margin-bottom:16px">
      <div style="display:flex;gap:10px;align-items:center;flex-wrap:wrap">
        <span style="font-size:13px;color:var(--el-text-color-regular)">快速生成：</span>
        <el-input-number v-model="genRows" :min="1" :max="20" size="small" /> <span style="font-size:13px">行</span>
        <el-input-number v-model="genCols" :min="1" :max="20" size="small" /> <span style="font-size:13px">列</span>
        <span style="font-size:13px">过道列</span>
        <el-input-number v-model="genAisle" :min="0" :max="19" size="small" />
        <el-button type="primary" @click="generate">生成网格</el-button>
        <el-divider direction="vertical" />
        <el-switch v-model="editMode" active-text="手动编辑模式" />
      </div>
    </el-card>

    <el-card v-if="editMode" shadow="never" style="margin-bottom:16px;border:1px solid #8f5bff33">
      <div style="display:flex;gap:14px;align-items:center;flex-wrap:wrap">
        <span style="font-weight:600">🖌️ 画笔：</span>
        <el-radio-group v-model="paint">
          <el-radio-button label="SEAT">座位</el-radio-button>
          <el-radio-button label="AISLE">过道</el-radio-button>
          <el-radio-button label="EMPTY">空位</el-radio-button>
          <el-radio-button label="DISABLED">禁用位</el-radio-button>
        </el-radio-group>
        <span style="font-size:12px;color:var(--el-text-color-secondary)">点击（或拖动）格子即可绘制；座位号保存时按行自动编号</span>
        <el-button size="small" @click="addRow">+ 行</el-button>
        <el-button size="small" @click="addCol">+ 列</el-button>
        <el-button type="primary" :loading="saving" style="margin-left:auto" @click="save">保存布局</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="seat-grid" :style="{ gridTemplateColumns: `repeat(${cols}, 40px)` }"
           @mouseup="painting = false" @mouseleave="painting = false">
        <div v-for="cell in cells" :key="cell.rowIndex + '-' + cell.colIndex"
             class="seat-cell"
             :class="cellClass(cell)"
             @mousedown="onDown(cell, $event)" @mouseenter="onEnter(cell)" @click="onClick(cell)"
             @contextmenu.prevent="onContext(cell, $event)">
          <span v-if="cell.cellType==='SEAT'">{{ shortNo(cell.seatNo) || '座' }}</span>
          <span v-else-if="cell.cellType==='AISLE'">·</span>
          <span v-else-if="cell.cellType==='DISABLED'">✕</span>
          <div v-if="cell.cellType==='SEAT' && (cell.tags && cell.tags.length)" class="seat-tags">
            <span v-for="k in cell.tags" :key="k" class="seat-tag-badge">{{ tagShort(k) }}</span>
          </div>
        </div>
      </div>
      <div class="legend" style="margin-top:16px">
        <div class="legend-item"><span class="legend-dot seat-FREE"></span>启用座位</div>
        <div class="legend-item"><span class="legend-dot seat-DISABLED"></span>禁用位</div>
        <div class="legend-item"><span class="legend-dot" style="background:transparent;border:1px dashed #ccc"></span>过道</div>
        <div class="legend-item"><span class="legend-dot" style="background:transparent;border:1px solid #eee"></span>空位</div>
        <div class="legend-item" style="color:var(--el-text-color-secondary)">右键座位可编辑属性</div>
      </div>
    </el-card>

    <!-- 右键座位属性编辑菜单 -->
    <template v-if="menuVisible">
      <div class="tag-menu-backdrop" @click="closeMenu" @contextmenu.prevent="closeMenu"></div>
      <div class="tag-menu" :style="{ left: menuX + 'px', top: menuY + 'px' }" @click.stop @contextmenu.prevent>
        <div class="tag-menu-title">座位属性 · {{ menuCell?.seatNo || '未编号' }}</div>
        <el-checkbox-group v-model="menuCell.tags" class="tag-menu-group">
          <el-checkbox v-for="t in SEAT_TAGS" :key="t.key" :label="t.key" class="tag-menu-item">{{ t.label }}</el-checkbox>
        </el-checkbox-group>
        <div class="tag-menu-hint">改动随「保存布局」生效</div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { baseApi } from '../../api'
import { SEAT_TAGS, tagShort, toTagKeys } from '../../constants/seatTags'

const route = useRoute()
const roomId = route.params.roomId
const cells = ref([])
const cols = ref(8)
const rows = ref(6)
const genRows = ref(6)
const genCols = ref(8)
const genAisle = ref(3)
const editMode = ref(false)
const paint = ref('SEAT')
const saving = ref(false)
const painting = ref(false)

// 右键属性菜单
const menuVisible = ref(false)
const menuX = ref(0)
const menuY = ref(0)
const menuCell = ref(null)
function onContext(cell, e) {
  if (cell.cellType !== 'SEAT') return
  if (!cell.tags) cell.tags = []
  menuCell.value = cell
  menuX.value = e.clientX
  menuY.value = e.clientY
  menuVisible.value = true
}
function closeMenu() { menuVisible.value = false; menuCell.value = null }
function onKey(e) { if (e.key === 'Escape') closeMenu() }
onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))

function cellClass(cell) {
  const base = editMode.value ? 'clickable' : (cell.cellType === 'SEAT' ? 'clickable' : '')
  if (cell.cellType === 'SEAT') return [cell.enabled ? 'seat-FREE' : 'seat-DISABLED', base]
  if (cell.cellType === 'DISABLED') return ['seat-DISABLED', base]
  if (cell.cellType === 'AISLE') return ['seat-AISLE', base]
  return ['seat-EMPTY', base]
}

function applyPaint(cell) {
  if (paint.value === 'SEAT') { cell.cellType = 'SEAT'; cell.enabled = 1; if (!cell.tags) cell.tags = [] }
  else if (paint.value === 'DISABLED') { cell.cellType = 'SEAT'; cell.enabled = 0; if (!cell.tags) cell.tags = [] }
  else { cell.cellType = paint.value; cell.enabled = 1; cell.seatNo = null; cell.tags = [] }
}
function onDown(cell, e) { if (e && e.button !== 0) return; if (!editMode.value) return; painting.value = true; applyPaint(cell) }
function onEnter(cell) { if (editMode.value && painting.value) applyPaint(cell) }
async function onClick(cell) {
  if (editMode.value) return   // 编辑模式下由 mousedown 处理
  if (cell.cellType !== 'SEAT' || !cell.seatId) return
  const next = cell.enabled ? 0 : 1
  try {
    await baseApi.toggleSeat(cell.seatId, next)
    cell.enabled = next
    ElMessage.success(next ? '已启用 ' + cell.seatNo : '已禁用 ' + cell.seatNo)
  } catch (e) { /* 拦截器提示（如存在未来预约） */ }
}

function addRow() {
  const r = rows.value
  for (let c = 0; c < cols.value; c++) cells.value.push({ rowIndex: r, colIndex: c, cellType: 'EMPTY', enabled: 1, seatNo: null, tags: [] })
  rows.value++
}
function addCol() {
  const c = cols.value
  for (let r = 0; r < rows.value; r++) cells.value.push({ rowIndex: r, colIndex: c, cellType: 'EMPTY', enabled: 1, seatNo: null, tags: [] })
  cols.value++
  reorder()
}
function reorder() { cells.value.sort((a, b) => (a.rowIndex - b.rowIndex) || (a.colIndex - b.colIndex)) }

async function generate() {
  try {
    await baseApi.generateLayout(roomId, { rows: genRows.value, cols: genCols.value, aisleCol: genAisle.value })
    ElMessage.success('已生成 ' + genRows.value + '×' + genCols.value + ' 座位网格')
    await load()
  } catch (e) { /* 拦截器提示 */ }
}

async function save() {
  // 按行为 SEAT 自动编号
  const perRow = {}
  const payload = cells.value.map(c => {
    let seatNo = null
    if (c.cellType === 'SEAT') {
      perRow[c.rowIndex] = (perRow[c.rowIndex] || 0) + 1
      seatNo = String.fromCharCode(65 + c.rowIndex) + '-' + String(perRow[c.rowIndex]).padStart(2, '0')
    }
    return {
      seatId: c.seatId ?? null,
      rowIndex: c.rowIndex, colIndex: c.colIndex, cellType: c.cellType, enabled: c.enabled, seatNo,
      tags: c.cellType === 'SEAT' ? (c.tags || []).join(',') : null
    }
  })
  saving.value = true
  try {
    await baseApi.saveLayout(roomId, { cells: payload })
    ElMessage.success('布局已保存')
    editMode.value = false
    await load()
  } catch (e) { /* 拦截器提示（存在未来预约时拒绝） */ } finally { saving.value = false }
}

onMounted(load)
async function load() {
  const data = await baseApi.layout(roomId)
  cells.value = (data.cells || []).map(c => ({ ...c, tags: toTagKeys(c.tags) }))
  cols.value = data.cols || 8
  rows.value = data.rows || 6
}
function shortNo(no) {
  if (!no) return ''
  const p = no.split('-')
  return p.length > 1 ? p[0] + p[1] : no
}
</script>

<style scoped>
.seat-EMPTY { border: 1px dashed var(--el-border-color-lighter) !important; }
.seat-cell { user-select: none; }

.tag-menu-backdrop { position: fixed; inset: 0; z-index: 3000; }
.tag-menu {
  position: fixed; z-index: 3001; min-width: 150px;
  background: var(--el-bg-color-overlay); border: 1px solid var(--el-border-color);
  border-radius: 8px; box-shadow: 0 8px 24px rgba(31,45,80,.18);
  padding: 10px 12px;
}
.tag-menu-title { font-weight: 600; font-size: 13px; margin-bottom: 8px; }
.tag-menu-group { display: flex; flex-direction: column; align-items: flex-start; }
.tag-menu-item { display: block; margin: 0 0 2px 0; }
.tag-menu-hint { font-size: 11px; color: var(--el-text-color-secondary); margin-top: 6px; }
</style>
