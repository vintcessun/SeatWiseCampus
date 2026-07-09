<template>
  <div class="page">
    <div class="page-title">我的候补</div>
    <div class="page-sub">
      座位满员时可加入候补。一旦有人取消 / 超时 / 签退，系统会自动为你保留座位并推送通知，
      请在 <b>倒计时结束前</b>确认，否则顺延给下一位。
    </div>

    <el-card shadow="never">
      <el-table :data="list" style="width:100%">
        <el-table-column prop="roomName" label="自习室" width="160" />
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column label="时段" width="130">
          <template #default="{ row }">{{ row.startText }} - {{ row.endText }}</template>
        </el-table-column>
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="dark" v-if="row.status==='OFFERED'">
              🔒 席位已保留 {{ remain(row) }}s
            </el-tag>
            <el-tag :type="statusType(row.status)" v-else>{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button v-if="row.status==='OFFERED'" size="small" type="success"
              :disabled="remain(row) <= 0" @click="accept(row)">立即确认预约</el-button>
            <el-button v-if="['WAITING','OFFERED'].includes(row.status)" size="small" type="danger" plain
              @click="cancel(row)">放弃候补</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!list.length" description="暂无候补记录，满员时可在选座页加入候补" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { waitlistApi, baseApi } from '../../api'
import { burstConfetti } from '../../utils/confetti'

const router = useRouter()
const list = ref([])
const nowMs = ref(Date.now())
const roomNames = ref({})
let ticker = null

const SLOT_MIN = 30
function slotText(slot) {
  const m = slot * SLOT_MIN
  return `${String(Math.floor(m / 60) % 24).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`
}

const statusMap = {
  WAITING: ['排队中', 'warning'],
  OFFERED: ['已为你保留', 'success'],
  FULFILLED: ['已预约', 'success'],
  CANCELLED: ['已放弃', 'info'],
  EXPIRED: ['已过期', 'info']
}
function statusText(s) { return statusMap[s]?.[0] || s }
function statusType(s) { return statusMap[s]?.[1] || '' }
function remain(row) {
  if (!row.offerExpireAt) return 0
  return Math.max(0, Math.ceil((new Date(row.offerExpireAt).getTime() - nowMs.value) / 1000))
}

onMounted(async () => {
  try {
    const rooms = await baseApi.rooms({})
    const map = {}
    ;(rooms || []).forEach(r => { map[r.id] = r.name })
    roomNames.value = map
  } catch (e) { /* 忽略，退回 roomId */ }
  await load()
  ticker = setInterval(() => { nowMs.value = Date.now() }, 1000)
})
onBeforeUnmount(() => { if (ticker) clearInterval(ticker) })

async function load() {
  const data = await waitlistApi.mine()
  list.value = (data || []).map(w => ({
    ...w,
    roomName: roomNames.value[w.roomId] || `自习室#${w.roomId}`,
    startText: slotText(w.startSlot),
    endText: slotText(w.endSlot)
  }))
}

async function accept(row) {
  try {
    await waitlistApi.accept(row.id)
    ElMessage.success('候补确认成功，座位已锁定，请按时签到')
    burstConfetti()
    router.push('/student/reservations')
  } catch (e) {
    ElMessage.warning(e?.message || '确认失败，席位可能已释放')
    await load()
  }
}

async function cancel(row) {
  try {
    await ElMessageBox.confirm('确认放弃该候补？', '提示', { type: 'warning' })
    await waitlistApi.cancel(row.id)
    ElMessage.success('已放弃候补')
    await load()
  } catch (e) {
    if (e !== 'cancel') await load()
  }
}
</script>
