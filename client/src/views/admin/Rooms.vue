<template>
  <div class="page">
    <div style="display:flex;justify-content:space-between;align-items:center">
      <div>
        <div class="page-title">自习室与座位</div>
        <div class="page-sub">新增自习室、维护座位排布（生成/启用/禁用）、查看实时看板</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增自习室</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="rooms" style="width:100%">
        <el-table-column prop="name" label="自习室" />
        <el-table-column label="楼栋/楼层" width="200">
          <template #default="{ row }">{{ buildingName(row.buildingId) }} · {{ row.floorNo }} 楼</template>
        </el-table-column>
        <el-table-column label="开放时间" width="160">
          <template #default="{ row }">{{ fmt(row.openStart) }} - {{ fmt(row.openEnd) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status==='OPEN'?'success':'info'">{{ row.status==='OPEN'?'开放':'关闭' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/admin/rooms/${row.id}/layout`)">座位排布</el-button>
            <el-button size="small" type="primary" @click="$router.push(`/admin/rooms/${row.id}/board`)">实时看板</el-button>
            <el-button size="small" :type="row.status==='OPEN'?'warning':'success'" plain @click="toggleStatus(row)">
              {{ row.status==='OPEN' ? '临时关闭' : '重新开放' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" title="新增自习室" width="440px">
      <el-form label-width="90px">
        <el-form-item label="所属楼栋">
          <el-select v-model="form.buildingId" placeholder="选择楼栋" style="width:100%">
            <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" placeholder="如 A303 静音自习室" /></el-form-item>
        <el-form-item label="楼层"><el-input-number v-model="form.floorNo" :min="1" :max="30" /></el-form-item>
        <el-form-item label="开放时间">
          <el-time-select v-model="form.openStart" start="06:00" end="12:00" step="00:30" style="width:120px" />
          <span style="margin:0 8px">至</span>
          <el-time-select v-model="form.openEnd" start="12:00" end="23:30" step="00:30" style="width:120px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { baseApi } from '../../api'

const rooms = ref([])
const buildings = ref([])
const dialog = ref(false)
const saving = ref(false)
const form = reactive({ buildingId: null, name: '', floorNo: 1, openStart: '08:00', openEnd: '22:00' })

onMounted(loadAll)
async function loadAll() {
  const campuses = await baseApi.campuses()
  let bs = []
  for (const c of campuses) bs = bs.concat(await baseApi.buildings(c.id))
  buildings.value = bs
  rooms.value = await baseApi.rooms({})
}
function buildingName(id) { return buildings.value.find(b => b.id === id)?.name || '' }
function fmt(t) { return t ? String(t).slice(0, 5) : '' }

async function toggleStatus(row) {
  const toClose = row.status === 'OPEN'
  try {
    if (toClose) {
      await ElMessageBox.confirm(
        `确认临时关闭「${row.name}」？关闭后将暂停新预约，并自动通知有未来预约的学生。`,
        '临时关闭自习室', { type: 'warning', confirmButtonText: '确认关闭' })
    }
    const res = await baseApi.setRoomStatus(row.id, toClose ? 'CLOSED' : 'OPEN')
    ElMessage.success(toClose ? `已关闭，已通知 ${res.affected} 位受影响学生` : '已重新开放')
    await loadAll()
  } catch (e) { if (e !== 'cancel') { /* 拦截器提示 */ } }
}

function openCreate() {
  if (buildings.value.length) form.buildingId = buildings.value[0].id
  dialog.value = true
}
async function save() {
  if (!form.buildingId || !form.name) { ElMessage.warning('请填写楼栋与名称'); return }
  saving.value = true
  try {
    await baseApi.createRoom({
      buildingId: form.buildingId, name: form.name, floorNo: form.floorNo,
      openStart: form.openStart + ':00', openEnd: form.openEnd + ':00', status: 'OPEN'
    })
    ElMessage.success('创建成功，可进入「座位排布」生成座位')
    dialog.value = false
    await loadAll()
  } catch (e) { /* 拦截器提示 */ } finally { saving.value = false }
}
</script>
