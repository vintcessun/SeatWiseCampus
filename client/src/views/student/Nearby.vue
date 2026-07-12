<template>
  <div class="page">
    <div class="page-title">附近有空位的自习室</div>
    <div class="page-sub">选择手动选楼栋或定位查找，按「距离最近 &gt; 空位更多」推荐</div>

    <el-card shadow="never" style="margin-bottom:16px">
      <div style="display:flex;gap:12px;flex-wrap:wrap;align-items:center">
        <el-radio-group v-model="locateMode" size="small">
          <el-radio-button value="manual">自定义楼栋</el-radio-button>
          <el-radio-button value="gps">定位查找</el-radio-button>
        </el-radio-group>

        <template v-if="locateMode === 'manual'">
          <el-select v-model="buildingId" placeholder="我当前在" style="width:200px">
            <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </template>
        <template v-else>
          <span v-if="locatedBuilding" style="font-size:13px;color:var(--el-text-color-secondary)">
            📍 {{ locatedBuilding.name }}
          </span>
          <span v-else style="font-size:13px;color:var(--el-text-color-secondary)">先点击「定位」获取位置</span>
        </template>

        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" />
        <el-time-select v-model="start" start="08:00" end="21:30" step="00:30" style="width:120px" />
        <el-time-select v-model="end" start="08:30" end="22:00" step="00:30" style="width:120px" />
        <el-button v-if="locateMode === 'gps'" :icon="Position" :loading="locating" @click="locateMe">定位</el-button>
        <el-button type="primary" :icon="LocationInformation" @click="search">推荐</el-button>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col v-for="r in list" :key="r.roomId" :span="8" style="margin-bottom:16px">
        <el-card shadow="hover">
          <div style="display:flex;justify-content:space-between">
            <div style="font-weight:700;font-size:16px">{{ r.roomName }}</div>
            <el-tag v-if="r.distance != null && r.distance < 9999" type="info">
              {{ Math.round(r.distance) > 0 ? Math.round(r.distance) + ' m' : '< 1 m' }}
            </el-tag>
            <el-tag v-else type="info">-- m</el-tag>
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
import { LocationInformation, Position } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { baseApi, nearbyApi } from '../../api'
import { todayLocal } from '../../utils/date'

const router = useRouter()
const buildings = ref([])
const buildingId = ref(null)
const date = ref(todayLocal())
const start = ref('14:00')
const end = ref('16:00')
const list = ref([])
const searched = ref(false)
const locating = ref(false)
const userLat = ref(null)
const userLng = ref(null)
const locateMode = ref('manual')
const locatedBuilding = ref(null)

// 浏览器定位 → 就近选择楼栋（Haversine）
function haversine(lat1, lng1, lat2, lng2) {
  const R = 6371000, toRad = d => d * Math.PI / 180
  const dLat = toRad(lat2 - lat1), dLng = toRad(lng2 - lng1)
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2
  return 2 * R * Math.asin(Math.sqrt(a))
}
function locateMe() {
  if (!navigator.geolocation) { ElMessage.warning('浏览器不支持定位，请使用自定义楼栋'); return }
  locating.value = true
  navigator.geolocation.getCurrentPosition(pos => {
    const { latitude, longitude } = pos.coords
    userLat.value = latitude
    userLng.value = longitude
    const withGeo = buildings.value.filter(b => b.latitude != null && b.longitude != null)
    if (!withGeo.length) { ElMessage.warning('楼栋暂无坐标，请联系管理员在「位置管理」维护'); locating.value = false; return }
    let best = null, bestD = Infinity
    for (const b of withGeo) {
      const d = haversine(latitude, longitude, Number(b.latitude), Number(b.longitude))
      if (d < bestD) { bestD = d; best = b }
    }
    locatedBuilding.value = best
    buildingId.value = best.id
    ElMessage.success(`已定位到最近楼栋：${best.name}（约 ${Math.round(bestD)} m）`)
    locating.value = false
    if (locateMode.value === 'gps') search()
  }, err => {
    ElMessage.warning('定位失败或被拒绝，请使用自定义楼栋')
    locating.value = false
  }, { enableHighAccuracy: true, timeout: 8000 })
}

onMounted(async () => {
  const campuses = await baseApi.campuses()
  if (campuses.length) {
    buildings.value = await baseApi.buildings(campuses[0].id)
    if (buildings.value.length) buildingId.value = buildings.value[0].id
  }
})

async function search() {
  if (locateMode.value === 'gps' && !userLat.value) {
    ElMessage.warning('请先点击「定位」获取位置')
    return
  }
  searched.value = true
  let lat = userLat.value, lng = userLng.value
  if (lat == null || lng == null) {
    const b = buildings.value.find(x => x.id === buildingId.value)
    if (b && b.latitude != null && b.longitude != null) {
      lat = Number(b.latitude); lng = Number(b.longitude)
    }
  }
  try {
    list.value = await nearbyApi.nearest({ originBuildingId: buildingId.value, date: date.value, start: start.value, end: end.value, userLat: lat, userLng: lng })
  } catch (e) {
    list.value = []
  }
}
function go(r) { router.push(`/student/rooms/${r.roomId}/seats`) }
</script>
