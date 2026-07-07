<template>
  <div class="page">
    <div class="page-title">自习室与座位</div>
    <div class="page-sub">维护座位排布（启用/禁用），查看实时座位看板</div>

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
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/admin/rooms/${row.id}/layout`)">座位排布</el-button>
            <el-button size="small" type="primary" @click="$router.push(`/admin/rooms/${row.id}/board`)">实时看板</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { baseApi } from '../../api'
const rooms = ref([])
const buildings = ref([])
onMounted(async () => {
  const campuses = await baseApi.campuses()
  let bs = []
  for (const c of campuses) bs = bs.concat(await baseApi.buildings(c.id))
  buildings.value = bs
  rooms.value = await baseApi.rooms({})
})
function buildingName(id) { return buildings.value.find(b => b.id === id)?.name || '' }
function fmt(t) { return t ? String(t).slice(0, 5) : '' }
</script>
