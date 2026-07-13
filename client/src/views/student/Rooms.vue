<template>
  <div class="page">
    <div class="page-title">{{ $t('rooms.title') }}</div>
    <div class="page-sub">{{ $t('rooms.subtitle') }}</div>

    <el-card shadow="never" style="margin-bottom:16px">
      <div style="display:flex;gap:12px;flex-wrap:wrap;align-items:center">
        <el-select v-model="campusId" :placeholder="$t('rooms.campus')" style="width:180px" @change="onCampus">
          <el-option v-for="c in campuses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="buildingId" :placeholder="$t('rooms.building')" style="width:180px" @change="onBuilding" clearable>
          <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
        </el-select>
        <el-select v-model="floorNo" :placeholder="$t('rooms.floor')" style="width:140px" @change="loadRooms" clearable>
          <el-option v-for="f in floors" :key="f" :label="$t('rooms.floorN', { n: f })" :value="f" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="loadRooms">{{ $t('rooms.search') }}</el-button>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col v-for="room in rooms" :key="room.id" :span="8" style="margin-bottom:16px">
        <el-card shadow="hover">
          <div style="display:flex;justify-content:space-between;align-items:start">
            <div>
              <div style="font-size:16px;font-weight:700">{{ room.name }}</div>
              <div style="color:#8a93a6;font-size:13px;margin-top:6px">
                {{ buildingName(room.buildingId) }} · {{ $t('rooms.floorN', { n: room.floorNo }) }}
              </div>
              <div style="color:#8a93a6;font-size:13px">{{ $t('rooms.openLabel') }} {{ fmt(room.openStart) }} - {{ fmt(room.openEnd) }}</div>
            </div>
            <el-tag :type="room.status==='OPEN'?'success':'info'">{{ room.status==='OPEN'?$t('rooms.open'):$t('rooms.closed') }}</el-tag>
          </div>
          <el-button type="primary" plain style="width:100%;margin-top:14px" @click="enter(room)">{{ $t('rooms.enter') }}</el-button>
        </el-card>
      </el-col>
      <el-empty v-if="!rooms.length" :description="$t('rooms.empty')" style="width:100%" />
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { baseApi } from '../../api'

const router = useRouter()
const campuses = ref([])
const buildings = ref([])
const rooms = ref([])
const campusId = ref(null)
const buildingId = ref(null)
const floorNo = ref(null)
const floors = ref([])

onMounted(async () => {
  campuses.value = await baseApi.campuses()
  if (campuses.value.length) {
    campusId.value = campuses.value[0].id
    await onCampus()
  }
})

async function onCampus() {
  buildingId.value = null
  floorNo.value = null
  buildings.value = campusId.value ? await baseApi.buildings(campusId.value) : []
  await loadRooms()
}
async function onBuilding() {
  floorNo.value = null
  await loadRooms()
}
async function loadRooms() {
  rooms.value = await baseApi.rooms({ campusId: campusId.value, buildingId: buildingId.value, floorNo: floorNo.value })
  floors.value = [...new Set(rooms.value.map(r => r.floorNo))].sort()
}
function buildingName(id) {
  return buildings.value.find(b => b.id === id)?.name || ''
}
function fmt(t) { return t ? String(t).slice(0, 5) : '' }
function enter(room) {
  router.push(`/student/rooms/${room.id}/seats`)
}
</script>
