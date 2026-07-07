<template>
  <div class="page">
    <div class="page-title">数据报表</div>
    <div class="page-sub">预约状态分布、热门时段、自习室利用率排行</div>

    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6"><el-card shadow="never"><div class="stat-card"><div class="num">{{ data.total || 0 }}</div><div class="lbl">总预约数</div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="stat-card"><div class="num" style="color:#3b6cff">{{ data.cancelRate || 0 }}%</div><div class="lbl">取消率</div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="stat-card"><div class="num" style="color:#d64545">{{ data.noShowRate || 0 }}%</div><div class="lbl">爽约率</div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="stat-card"><div class="num" style="color:#1f9d55">{{ topRoom }}</div><div class="lbl">最热自习室</div></div></el-card></el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="10"><el-card shadow="never"><div ref="pieEl" style="height:320px"></div></el-card></el-col>
      <el-col :span="14"><el-card shadow="never"><div ref="barEl" style="height:320px"></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="24"><el-card shadow="never"><div ref="rankEl" style="height:320px"></div></el-card></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { reportApi } from '../../api'

const data = reactive({})
const pieEl = ref(); const barEl = ref(); const rankEl = ref()
const topRoom = computed(() => data.roomRanking?.[0]?.reservationCount ? data.roomRanking[0].roomName : '-')

const statusLabel = {
  PENDING_SIGN_IN: '待签到', IN_USE: '使用中', COMPLETED: '已完成',
  CANCELLED: '已取消', EXPIRED_RELEASED: '爽约释放'
}

onMounted(async () => {
  Object.assign(data, await reportApi.summary())
  await nextTick()
  renderPie(); renderBar(); renderRank()
})

function renderPie() {
  const chart = echarts.init(pieEl.value)
  const sd = data.statusDistribution || {}
  chart.setOption({
    title: { text: '预约状态分布', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '68%'], center: ['50%', '46%'],
      data: Object.keys(sd).map(k => ({ name: statusLabel[k] || k, value: sd[k] }))
    }]
  })
}
function renderBar() {
  const chart = echarts.init(barEl.value)
  const peak = data.peakSlots || []
  chart.setOption({
    title: { text: '热门时段', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: {},
    xAxis: { type: 'category', data: peak.map(p => p.timeLabel) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: peak.map(p => p.count), itemStyle: { color: '#3b6cff', borderRadius: [4, 4, 0, 0] } }]
  })
}
function renderRank() {
  const chart = echarts.init(rankEl.value)
  const rank = [...(data.roomRanking || [])].reverse()
  chart.setOption({
    title: { text: '自习室利用率排行（预约数）', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: {},
    grid: { left: 120 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: rank.map(r => r.roomName) },
    series: [{ type: 'bar', data: rank.map(r => r.reservationCount), itemStyle: { color: '#8f5bff', borderRadius: [0, 4, 4, 0] } }]
  })
}
</script>
