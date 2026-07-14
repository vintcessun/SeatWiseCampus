<template>
  <div class="page">
    <div style="display:flex;justify-content:space-between;align-items:center">
      <div>
        <div class="page-title">位置管理</div>
        <div class="page-sub">新增楼栋、维护楼栋经纬度 — 点击地图选点，或手动输入坐标</div>
      </div>
      <el-button type="primary" :icon="OfficeBuilding" @click="bldDialog = true">新增楼栋</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="buildings" style="width:100%">
        <el-table-column prop="name" label="楼栋" width="200" />
        <el-table-column label="校区" width="140">
          <template #default="{ row }">{{ campusName(row.campusId) }}</template>
        </el-table-column>
        <el-table-column label="纬度" width="180">
          <template #default="{ row }">
            <el-input-number v-model="row.latitude" :precision="6" :step="0.0001" :controls="false" style="width:150px" @change="onEdit(row)" />
          </template>
        </el-table-column>
        <el-table-column label="经度" width="180">
          <template #default="{ row }">
            <el-input-number v-model="row.longitude" :precision="6" :step="0.0001" :controls="false" style="width:150px" @change="onEdit(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <div style="white-space:nowrap">
              <el-button size="small" type="primary" @click="pickOnMap(row)">地图选点</el-button>
              <el-button size="small" @click="save(row)">保存坐标</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="bldDialog" title="新增楼栋" width="420px">
      <el-form label-width="90px">
        <el-form-item label="所属校区">
          <el-select v-model="bldForm.campusId" placeholder="选择校区" style="width:100%">
            <el-option v-for="c in campuses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋名称"><el-input v-model="bldForm.name" placeholder="如 图书馆B座" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bldDialog=false">取消</el-button>
        <el-button type="primary" :loading="bldSaving" @click="saveBuilding">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="mapDialog" :title="'地图选点 — ' + (mapTarget?.name || '')" width="700px" @opened="openMap" @closed="closeMap">
      <div ref="mapContainer" style="height:400px;width:100%;border-radius:8px"></div>
      <div v-if="mapLat !== null" style="margin-top:10px;font-size:14px;color:#606266">
        已选坐标：纬度 <b>{{ mapLat }}</b>，经度 <b>{{ mapLng }}</b>
      </div>
      <template #footer>
        <el-button @click="mapDialog = false">取消</el-button>
        <el-button type="primary" :disabled="mapLat === null" @click="confirmMapPick">确认坐标</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { OfficeBuilding } from '@element-plus/icons-vue'
import { baseApi } from '../../api'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
// 修复 Vite 构建下 Leaflet 默认标记图标加载失败
import iconUrl from 'leaflet/dist/images/marker-icon.png'
import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png'
import shadowUrl from 'leaflet/dist/images/marker-shadow.png'

L.Icon.Default.mergeOptions({ iconUrl, iconRetinaUrl, shadowUrl })

const buildings = ref([])
const campuses = ref([])
const bldDialog = ref(false)
const bldSaving = ref(false)
const bldForm = reactive({ campusId: null, name: '' })
const mapDialog = ref(false)
const mapTarget = ref(null)
const mapContainer = ref(null)
const mapLat = ref(null)
const mapLng = ref(null)
let mapInstance = null
let mapMarker = null

onMounted(loadAll)
async function loadAll() {
  campuses.value = await baseApi.campuses()
  let bs = []
  for (const c of campuses.value) bs = bs.concat(await baseApi.buildings(c.id))
  buildings.value = bs.map(b => ({
    ...b,
    latitude: b.latitude != null ? Number(b.latitude) : null,
    longitude: b.longitude != null ? Number(b.longitude) : null
  }))
  if (!bldForm.campusId && campuses.value.length) bldForm.campusId = campuses.value[0].id
}
function campusName(id) { return campuses.value.find(c => c.id === id)?.name || '' }

async function saveBuilding() {
  if (!bldForm.campusId || !bldForm.name) { ElMessage.warning('请选择校区并填写名称'); return }
  bldSaving.value = true
  try {
    await baseApi.createBuilding({ campusId: bldForm.campusId, name: bldForm.name })
    ElMessage.success('楼栋已创建，请为其设置坐标')
    bldDialog.value = false
    bldForm.name = ''
    await loadAll()
  } catch (e) { /* 拦截器提示 */ } finally { bldSaving.value = false }
}

const dirty = new Set()
function onEdit(row) { dirty.add(row.id) }

async function save(row) {
  if (row.latitude == null || row.longitude == null) { ElMessage.warning('请填写或选择经纬度'); return }
  try {
    await baseApi.updateBuildingLocation(row.id, row.latitude, row.longitude)
    ElMessage.success(`已保存「${row.name}」坐标`)
    dirty.delete(row.id)
  } catch (e) { /* 拦截器提示 */ }
}

// ============ 地图选点（Leaflet + OpenStreetMap 瓦片）============
function pickOnMap(row) {
  mapTarget.value = row
  mapLat.value = row.latitude
  mapLng.value = row.longitude
  mapDialog.value = true
}

function openMap() {
  const el = mapContainer.value
  if (!el) return

  // 用多层 requestAnimationFrame + setTimeout 确保 dialog 动画完成后才创建地图
  const tryInit = (attempt) => {
    const w = el.offsetWidth
    const h = el.offsetHeight
    if (w > 50 && h > 50) {
      doInitMap()
      return
    }
    if (attempt < 10) {
      requestAnimationFrame(() => tryInit(attempt + 1))
    } else {
      // 10 次后强制初始化
      doInitMap()
    }
  }

  requestAnimationFrame(() => tryInit(0))
}

function doInitMap() {
  const el = mapContainer.value
  if (!el || mapInstance) return

  const center = [24.605422, 118.313908]
  const hasCoord = mapLat.value != null && mapLng.value != null
  const initPos = hasCoord ? [mapLat.value, mapLng.value] : center

  mapInstance = L.map(el, { zoomControl: true }).setView(initPos, 16)

  // 高德地图瓦片（OSM 在国内被墙）
  L.tileLayer('https://webst{s}.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}', {
    attribution: '&copy; 高德地图',
    maxZoom: 18,
    subdomains: ['01', '02', '03', '04']
  }).addTo(mapInstance)

  mapInstance.invalidateSize()
  // 第二次 invalidateSize 确保完全渲染（dialog 动画有延迟）
  setTimeout(() => { if (mapInstance) mapInstance.invalidateSize() }, 200)

  if (hasCoord) {
    mapMarker = L.marker(initPos, { draggable: true }).addTo(mapInstance)
    mapMarker.on('dragend', (e) => {
      const pos = e.target.getLatLng()
      mapLat.value = parseFloat(pos.lat.toFixed(6))
      mapLng.value = parseFloat(pos.lng.toFixed(6))
    })
  }

  mapInstance.on('click', (e) => {
    const pos = e.latlng
    mapLat.value = parseFloat(pos.lat.toFixed(6))
    mapLng.value = parseFloat(pos.lng.toFixed(6))
    if (mapMarker) {
      mapMarker.setLatLng(pos)
    } else {
      mapMarker = L.marker(pos, { draggable: true }).addTo(mapInstance)
      mapMarker.on('dragend', (ev) => {
        const p = ev.target.getLatLng()
        mapLat.value = parseFloat(p.lat.toFixed(6))
        mapLng.value = parseFloat(p.lng.toFixed(6))
      })
    }
  })
}

function closeMap() {
  if (mapInstance) { mapInstance.remove(); mapInstance = null }
  mapMarker = null
}

function confirmMapPick() {
  if (mapTarget.value && mapLat.value != null && mapLng.value != null) {
    mapTarget.value.latitude = mapLat.value
    mapTarget.value.longitude = mapLng.value
    dirty.add(mapTarget.value.id)
    ElMessage.success(`已选择坐标：${mapLat.value}, ${mapLng.value}，记得点「保存坐标」`)
  }
  mapDialog.value = false
}
</script>

<style scoped>
</style>
