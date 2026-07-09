<template>
  <div class="page">
    <div class="page-title">📊 我的自习报告</div>
    <div class="page-sub">基于你的预约与签到记录自动统计，量化你的自习习惯</div>

    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6"><el-card shadow="never"><div class="kpi"><div class="v">{{ data.completedSessions }}</div><div class="k">累计完成场次</div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="kpi"><div class="v">{{ data.totalHours }}<small> h</small></div><div class="k">累计自习时长</div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="kpi"><div class="v">{{ data.streakDays }}<small> 天</small></div><div class="k">连续自习天数 🔥</div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="kpi"><div class="v" :style="{color: rateColor}">{{ data.onTimeRate }}%</div><div class="k">守约率</div></div></el-card></el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="15">
        <el-card shadow="never">
          <div class="card-title">近 7 天自习时长（小时）</div>
          <div ref="barEl" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="9">
        <el-card shadow="never" style="margin-bottom:16px">
          <div class="card-title">守约情况</div>
          <div ref="pieEl" style="height:220px"></div>
        </el-card>
        <el-card shadow="never">
          <div class="tip">
            <div>✅ 完成 <b>{{ data.completedSessions }}</b> 场，爽约释放 <b>{{ data.expiredSessions }}</b> 场</div>
            <div style="margin-top:6px;color:#8a93a6;font-size:13px">守时是一种美德——按时签到可加分，连续自习可冲击排行榜 🏆</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { meApi } from '../../api'

const data = reactive({ completedSessions: 0, expiredSessions: 0, totalHours: 0, onTimeRate: 100, streakDays: 0, weekly: [] })
const barEl = ref(null)
const pieEl = ref(null)
const rateColor = computed(() => data.onTimeRate >= 90 ? '#1f9d55' : data.onTimeRate >= 70 ? '#d98a00' : '#d64545')

function renderBar() {
  const chart = echarts.init(barEl.value)
  chart.setOption({
    grid: { left: 40, right: 16, top: 20, bottom: 30 },
    tooltip: { trigger: 'axis', valueFormatter: v => v + ' h' },
    xAxis: { type: 'category', data: data.weekly.map(w => w.date.slice(5)), axisLabel: { color: '#8a93a6' } },
    yAxis: { type: 'value', axisLabel: { color: '#8a93a6' } },
    series: [{
      type: 'bar', data: data.weekly.map(w => w.hours), barWidth: '46%',
      itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#5b8cff' }, { offset: 1, color: '#8f5bff' }]), borderRadius: [6, 6, 0, 0] }
    }]
  })
}
function renderPie() {
  const chart = echarts.init(pieEl.value)
  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['52%', '76%'], center: ['50%', '50%'], label: { formatter: '{b}\n{c}' },
      data: [
        { value: data.completedSessions, name: '按时完成', itemStyle: { color: '#1f9d55' } },
        { value: data.expiredSessions, name: '爽约释放', itemStyle: { color: '#d64545' } }
      ]
    }]
  })
}

onMounted(async () => {
  Object.assign(data, await meApi.studyReport())
  await nextTick()
  renderBar()
  renderPie()
})
</script>

<style scoped>
.kpi { text-align: center; padding: 6px 0; }
.kpi .v { font-size: 30px; font-weight: 800; color: #2b3350; }
.kpi .v small { font-size: 15px; font-weight: 600; }
.kpi .k { font-size: 13px; color: #8a93a6; margin-top: 4px; }
.card-title { font-weight: 700; margin-bottom: 12px; }
.tip { font-size: 14px; line-height: 1.7; }
</style>
