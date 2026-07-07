<template>
  <div class="page">
    <div class="page-title">附近有空位的自习室</div>
    <div class="page-sub">选择你当前所在楼栋，按「同楼栋 &gt; 距离最近 &gt; 空位更多」推荐</div>

    <el-card shadow="never" style="margin-bottom:16px">
      <div style="display:flex;gap:12px;flex-wrap:wrap;align-items:center">
        <el-select v-model="buildingId" placeholder="我当前在" style="width:200px">
          <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
        </el-select>
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" />
        <el-time-select v-model="start" start="08:00" end="21:30" step="00:30" style="width:120px" />
        <el-time-select v-model="end" start="08:30" end="22:00" step="00:30" style="width:120px" />
        <el-button type="primary" :icon="LocationInformation" @click="search">推荐</el-button>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col v-for="r in list" :key="r.roomId" :span="8" style="margin-bottom:16px">
        <el-card shadow="hover">
          <div style="display:flex;justify-content:space-between">
            <div style="font-weight:700;font-size:16px">{{ r.roomName }}</div>
            <el-tag v-if="r.sameBuilding" type="success">同楼栋</el-tag>
            <el-tag v-else type="info">{{ r.distance }} m</el-tag>
          </div>
          <div style="color:#8a93a6;font-size:13px;margin:8px 0">{{ r.buildingName }} · {{ r.floorNo }} 楼</div>
          <div style="font-size:22px;font-weight:800;color:#1f9d55">{{ r.availableSeats }} <span style="font-size:13px;color:#8a93a6">个空位</span></div>
          <el-button type="primary" plain style="width:100%;margin-top:10px" @click="go(r)">去预约</el-button>
        </el-card>
      </el-col>
      <el-empty v-if="searched && !list.length" description="附近暂无空位" style="width:100%" />
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { LocationInformation } from '@element-plus/icons-vue'
import { baseApi, nearbyApi } from '../../api'

const router = useRouter()
const buildings = ref([])
const buildingId = ref(null)
const date = ref(new Date().toISOString().slice(0, 10))
const start = ref('14:00')
const end = ref('16:00')
const list = ref([])
const searched = ref(false)

onMounted(async () => {
  const campuses = await baseApi.campuses()
  if (campuses.length) {
    buildings.value = await baseApi.buildings(campuses[0].id)
    if (buildings.value.length) buildingId.value = buildings.value[0].id
  }
})

async function search() {
  searched.value = true
  try {
    list.value = await nearbyApi.nearest({ originBuildingId: buildingId.value, date: date.value, start: start.value, end: end.value })
  } catch (e) {
    list.value = []
  }
}
function go(r) { router.push(`/student/rooms/${r.roomId}/seats`) }
</script>
