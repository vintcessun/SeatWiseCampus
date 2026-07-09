<template>
  <div class="page">
    <div class="page-title">学生预约追踪</div>
    <div class="page-sub">按姓名 / 用户名、状态、日期查询学生预约记录</div>

    <el-card shadow="never" style="margin-bottom:16px">
      <div style="display:flex;gap:12px;flex-wrap:wrap;align-items:center">
        <el-input v-model="keyword" placeholder="学生姓名 / 用户名" style="width:200px" clearable @keyup.enter="search" />
        <el-select v-model="status" placeholder="状态" style="width:150px" clearable>
          <el-option v-for="s in statuses" :key="s.v" :label="s.l" :value="s.v" />
        </el-select>
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" placeholder="日期" clearable />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" style="width:100%">
        <el-table-column prop="studentName" label="学生" width="110" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="roomName" label="自习室" width="160" />
        <el-table-column prop="seatNo" label="座位" width="80" />
        <el-table-column prop="date" label="日期" width="115" />
        <el-table-column label="时段" width="120">
          <template #default="{ row }">{{ row.startTime }} - {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="签到" width="100">
          <template #default="{ row }">{{ row.checkInTime ? fmtT(row.checkInTime) : '-' }}</template>
        </el-table-column>
        <el-table-column label="签退" width="100">
          <template #default="{ row }">{{ row.checkOutTime ? fmtT(row.checkOutTime) : '-' }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!list.length" description="无匹配记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { adminApi } from '../../api'

const keyword = ref('')
const status = ref('')
const date = ref('')
const list = ref([])
const statuses = [
  { v: 'PENDING_SIGN_IN', l: '待签到' }, { v: 'IN_USE', l: '使用中' },
  { v: 'COMPLETED', l: '已完成' }, { v: 'CANCELLED', l: '已取消' }, { v: 'EXPIRED_RELEASED', l: '爽约释放' }
]
const smap = Object.fromEntries(statuses.map(s => [s.v, s.l]))
const stype = { PENDING_SIGN_IN: 'warning', IN_USE: 'danger', COMPLETED: 'success', CANCELLED: 'info', EXPIRED_RELEASED: 'info' }
function statusText(s) { return smap[s] || s }
function statusType(s) { return stype[s] || '' }
function fmtT(t) { return t ? String(t).replace('T', ' ').slice(11, 16) : '-' }

onMounted(search)
async function search() {
  list.value = await adminApi.reservations({ keyword: keyword.value || undefined, status: status.value || undefined, date: date.value || undefined })
}
</script>
