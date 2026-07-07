<template>
  <div class="page">
    <div class="page-title">我的预约</div>
    <div class="page-sub">签到 / 签退 / 取消。超时未签到将自动释放并计入爽约</div>

    <el-card shadow="never">
      <el-table :data="list" style="width:100%">
        <el-table-column prop="roomName" label="自习室" width="160" />
        <el-table-column prop="seatNo" label="座位" width="90" />
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column label="时段" width="130">
          <template #default="{ row }">{{ row.startTime }} - {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button v-if="row.status==='PENDING_SIGN_IN'" size="small" type="success" @click="act('checkIn', row)">签到</el-button>
            <el-button v-if="row.status==='IN_USE'" size="small" type="warning" @click="act('checkOut', row)">签退</el-button>
            <el-button v-if="['PENDING_SIGN_IN','IN_USE'].includes(row.status)" size="small" type="danger" plain @click="act('cancel', row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!list.length" description="还没有预约，去选座吧" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reservationApi } from '../../api'

const list = ref([])

onMounted(load)
async function load() { list.value = await reservationApi.mine() }

const statusMap = {
  PENDING_SIGN_IN: ['待签到', 'warning'],
  IN_USE: ['使用中', 'danger'],
  COMPLETED: ['已完成', 'success'],
  CANCELLED: ['已取消', 'info'],
  EXPIRED_RELEASED: ['已释放(爽约)', 'info']
}
function statusText(s) { return statusMap[s]?.[0] || s }
function statusType(s) { return statusMap[s]?.[1] || '' }

async function act(type, row) {
  try {
    if (type === 'cancel') {
      await ElMessageBox.confirm('确认取消该预约？临近开始时间取消可能扣分', '提示', { type: 'warning' })
    }
    await reservationApi[type](row.id)
    ElMessage.success('操作成功')
    await load()
  } catch (e) {
    if (e !== 'cancel') { /* 拦截器已提示或用户取消 */ await load() }
  }
}
</script>
