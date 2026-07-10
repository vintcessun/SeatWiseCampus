<template>
  <div class="page">
    <div class="page-title">位置管理</div>
    <div class="page-sub">维护楼栋经纬度，供学生端「附近空位」按真实定位就近推荐（Haversine 距离）</div>

    <el-card shadow="never">
      <el-table :data="buildings" style="width:100%">
        <el-table-column prop="name" label="楼栋" width="200" />
        <el-table-column label="校区" width="140">
          <template #default="{ row }">{{ campusName(row.campusId) }}</template>
        </el-table-column>
        <el-table-column label="纬度 latitude" width="220">
          <template #default="{ row }">
            <el-input-number v-model="row.latitude" :precision="6" :step="0.0001" :controls="false" style="width:180px" />
          </template>
        </el-table-column>
        <el-table-column label="经度 longitude" width="220">
          <template #default="{ row }">
            <el-input-number v-model="row.longitude" :precision="6" :step="0.0001" :controls="false" style="width:180px" />
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="save(row)">保存坐标</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { baseApi } from '../../api'

const buildings = ref([])
const campuses = ref([])

onMounted(async () => {
  campuses.value = await baseApi.campuses()
  let bs = []
  for (const c of campuses.value) bs = bs.concat(await baseApi.buildings(c.id))
  buildings.value = bs.map(b => ({ ...b, latitude: b.latitude != null ? Number(b.latitude) : null, longitude: b.longitude != null ? Number(b.longitude) : null }))
})
function campusName(id) { return campuses.value.find(c => c.id === id)?.name || '' }

async function save(row) {
  if (row.latitude == null || row.longitude == null) { ElMessage.warning('请填写经纬度'); return }
  try {
    await baseApi.updateBuildingLocation(row.id, row.latitude, row.longitude)
    ElMessage.success(`已保存「${row.name}」坐标`)
  } catch (e) { /* 拦截器提示 */ }
}
</script>
