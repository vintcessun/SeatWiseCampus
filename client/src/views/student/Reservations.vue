<template>
  <div class="page">
    <div class="page-title">我的预约</div>
    <div class="page-sub">签到 / 签退 / 取消。签到需在「签到窗口」内完成，超时未签到自动释放并计入爽约</div>

    <el-card shadow="never">
      <el-table :data="list" style="width:100%">
        <el-table-column prop="roomName" label="自习室" width="150" />
        <el-table-column prop="seatNo" label="座位" width="80" />
        <el-table-column prop="date" label="日期" width="115" />
        <el-table-column label="时段" width="120">
          <template #default="{ row }">{{ row.startTime }} - {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column label="签到窗口" width="130">
          <template #default="{ row }">
            <span v-if="row.status==='PENDING_SIGN_IN'" style="color:#8a93a6;font-size:12px">
              {{ row.signinStart }} - {{ row.signinDeadline }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-tooltip v-if="row.status==='PENDING_SIGN_IN' && !canCheckIn(row)" content="未到签到时间" placement="top">
              <span><el-button size="small" type="success" disabled>签到</el-button></span>
            </el-tooltip>
            <el-button v-else-if="row.status==='PENDING_SIGN_IN'" size="small" type="success" @click="act('checkIn', row)">签到</el-button>
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
import { useUserStore } from '../../stores/user'

const list = ref([])
const user = useUserStore()

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

// 当前时间是否已进入签到窗口 [signinStart, signinDeadline]
function canCheckIn(row) {
  if (!row.signinStart) return true
  const now = new Date()
  const start = new Date(`${row.date}T${row.signinStart}:00`)
  const end = new Date(`${row.date}T${row.signinDeadline}:00`)
  return now >= start && now <= end
}

async function act(type, row) {
  try {
    if (type === 'cancel') {
      await ElMessageBox.confirm('确认取消该预约？临近开始时间取消可能扣分', '提示', { type: 'warning' })
    }
    const data = await reservationApi[type](row.id)
    if (data && data.scoreDelta) {
      ElMessage.success(`操作成功，积分 ${data.scoreDelta > 0 ? '+' : ''}${data.scoreDelta}`)
    } else {
      ElMessage.success('操作成功')
    }
    await user.refreshProfile().catch(() => {})  // 实时刷新右上角积分
    await load()
  } catch (e) {
    if (e !== 'cancel') { await load() }
  }
}
</script>
