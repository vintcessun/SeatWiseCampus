<template>
  <div class="page">
    <div class="hero">
      <div>
        <div class="hero-hi">管理控制台概览 📊</div>
        <div class="hero-sub">SeatWise · 实时掌握自习室运行状况</div>
      </div>
      <el-button class="hero-refresh" :icon="Refresh" :loading="loading" round @click="load">刷新数据</el-button>
    </div>

    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6"><el-card shadow="never"><div class="ov"><div class="ic" style="background:#eef4ff">🏫</div><div><div class="ov-v"><CountUp :value="rooms.length" /></div><div class="ov-k">自习室数</div></div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="ov"><div class="ic" style="background:#eafaf0">📅</div><div><div class="ov-v"><CountUp :value="data.total || 0" /></div><div class="ov-k">总预约数</div></div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="ov"><div class="ic" style="background:#e9f0ff">↩️</div><div><div class="ov-v" style="color:#3b6cff"><CountUp :value="data.cancelRate || 0" />%</div><div class="ov-k">取消率</div></div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="ov"><div class="ic" style="background:#ffefef">⚠️</div><div><div class="ov-v" style="color:#d64545"><CountUp :value="data.noShowRate || 0" />%</div><div class="ov-k">爽约率</div></div></div></el-card></el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="10">
        <el-card shadow="never"><div ref="pieEl" style="height:300px"></div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <div class="card-title">自习室实时空位</div>
          <div v-for="r in roomLive" :key="r.id" class="rl">
            <span class="rl-name">{{ r.name }}</span>
            <el-progress :percentage="r.pct" :color="r.pct > 50 ? '#1f9d55' : r.pct > 20 ? '#d98a00' : '#d64545'" :stroke-width="10" style="flex:1" />
            <span class="rl-num">{{ r.free }} 空</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="card-title">快捷入口</div>
          <div class="quick">
            <div class="q" @click="$router.push('/admin/rooms')"><span>🏫</span>自习室座位</div>
            <div class="q" @click="$router.push('/admin/students')"><span>👤</span>学生追踪</div>
            <div class="q" @click="$router.push('/admin/reports')"><span>📈</span>数据报表</div>
            <div class="q" @click="$router.push('/admin/blacklist')"><span>⚠️</span>黑名单</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Refresh } from '@element-plus/icons-vue'
import { reportApi, baseApi, boardApi } from '../../api'
import { todayLocal } from '../../utils/date'
import CountUp from '../../components/CountUp.vue'

const data = reactive({})
const rooms = ref([])
const roomLive = ref([])
const pieEl = ref()
const loading = ref(false)
const statusLabel = { PENDING_SIGN_IN: '待签到', IN_USE: '使用中', COMPLETED: '已完成', CANCELLED: '已取消', EXPIRED_RELEASED: '爽约释放' }

onMounted(load)
async function load() {
  loading.value = true
  try {
  Object.assign(data, await reportApi.summary().catch(() => ({})))
  rooms.value = await baseApi.rooms({}).catch(() => [])
  // 实时空位：当前时段各自习室快照
  const T = todayLocal()
  const now = new Date(); const h = now.getHours()
  const st = `${String(h).padStart(2, '0')}:00`, en = h >= 23 ? '23:59' : `${String(h + 1).padStart(2, '0')}:00`
  const live = []
  for (const r of rooms.value) {
    const b = await boardApi.snapshot(r.id, { date: T, start: st, end: en }).catch(() => null)
    if (b) {
      const seats = b.seats.filter(s => ['FREE', 'RESERVED', 'USING', 'HELD'].includes(s.status))
      const free = seats.filter(s => s.status === 'FREE').length
      live.push({ id: r.id, name: r.name, free, pct: seats.length ? Math.round(free / seats.length * 100) : 0 })
    }
  }
  roomLive.value = live
  await nextTick(); renderPie()
  } finally { loading.value = false }
}
function renderPie() {
  if (!pieEl.value) return
  const chart = echarts.init(pieEl.value)
  const sd = data.statusDistribution || {}
  chart.setOption({
    title: { text: '预约状态分布', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'item' }, legend: { bottom: 0 },
    series: [{ type: 'pie', radius: ['42%', '68%'], center: ['50%', '46%'], data: Object.keys(sd).map(k => ({ name: statusLabel[k] || k, value: sd[k] })) }]
  })
}
</script>

<style scoped>
.hero { background: linear-gradient(135deg, #1a2233, #3b3f6b); color: #fff; border-radius: 16px; padding: 22px 26px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.hero-refresh { background: rgba(255,255,255,.16); border: 1px solid rgba(255,255,255,.4); color: #fff; backdrop-filter: blur(6px); font-weight: 600; }
.hero-refresh:hover { background: rgba(255,255,255,.28); border-color: #fff; color: #fff; }
.hero-hi { font-size: 20px; font-weight: 800; }
.hero-sub { opacity: .85; margin-top: 6px; }
.ov { display: flex; align-items: center; gap: 14px; }
.ov .ic { width: 46px; height: 46px; border-radius: 12px; display: grid; place-items: center; font-size: 22px; }
.ov-v { font-size: 24px; font-weight: 800; }
.ov-k { font-size: 12px; color: #8a93a6; }
.card-title { font-weight: 700; margin-bottom: 12px; }
.rl { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; font-size: 13px; }
.rl-name { width: 110px; }
.rl-num { width: 44px; text-align: right; color: #5a6172; }
.quick { display: grid; gap: 10px; }
.q { background: #f7f8fc; border-radius: 12px; padding: 12px; text-align: center; cursor: pointer; font-weight: 600; }
.q span { display: block; font-size: 20px; margin-bottom: 4px; }
.q:hover { background: #eef2ff; color: #3b6cff; }
</style>
