<template>
  <div class="page">
    <!-- 欢迎横幅 -->
    <div class="hero">
      <div>
        <div class="hero-hi">{{ $t('home.greeting', { name: user.userInfo?.realName }) }}</div>
        <div class="hero-sub">{{ $t('home.welcome') }}</div>
      </div>
      <div class="hero-stats">
        <div class="hero-stat"><div class="v"><CountUp :value="user.userInfo?.creditScore ?? 0" /></div><div class="k">{{ $t('home.creditScore') }}</div></div>
        <div class="hero-stat"><div class="v">{{ myRank || '-' }}</div><div class="k">{{ $t('home.rankNo') }}</div></div>
      </div>
    </div>

    <!-- 公告横幅 -->
    <el-alert v-for="a in announcements" :key="a.id" :type="a.level === 'WARN' ? 'warning' : 'info'"
      :closable="false" show-icon style="margin-bottom:10px">
      <template #title>
        <span style="font-weight:700">📢 {{ a.title }}</span>
        <span style="margin-left:10px;color:#8a93a6;font-size:12px">{{ (a.createdTime || '').slice(0,10) }}</span>
      </template>
      <div style="font-size:13px">{{ a.content }}</div>
    </el-alert>

    <!-- 概览卡片 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6"><el-card shadow="never"><div class="ov"><div class="ic" style="background:#eef4ff">📅</div><div><div class="ov-v"><CountUp :value="stat.today" /></div><div class="ov-k">{{ $t('home.today') }}</div></div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="ov"><div class="ic" style="background:#ffefef">🪑</div><div><div class="ov-v"><CountUp :value="stat.inUse" /></div><div class="ov-k">{{ $t('home.inUse') }}</div></div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="ov"><div class="ic" style="background:#eafaf0">✅</div><div><div class="ov-v"><CountUp :value="stat.completed" /></div><div class="ov-k">{{ $t('home.completed') }}</div></div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="ov"><div class="ic" style="background:#fff6e0">⚠️</div><div><div class="ov-v"><CountUp :value="user.userInfo?.noShowCount ?? 0" /></div><div class="ov-k">{{ $t('home.noShow') }}</div></div></div></el-card></el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card shadow="never">
          <div class="card-title">{{ $t('home.upcoming') }}</div>
          <el-table :data="upcoming" style="width:100%" v-if="upcoming.length">
            <el-table-column prop="roomName" :label="$t('home.colRoom')" />
            <el-table-column prop="seatNo" :label="$t('home.colSeat')" width="80" />
            <el-table-column :label="$t('home.colTime')" width="140"><template #default="{ row }">{{ row.date }} {{ row.startTime }}-{{ row.endTime }}</template></el-table-column>
            <el-table-column :label="$t('home.colStatus')" width="100"><template #default="{ row }"><el-tag :type="row.status==='IN_USE'?'danger':'warning'">{{ row.status==='IN_USE'?$t('home.statusUsing'):$t('home.statusPending') }}</el-tag></template></el-table-column>
          </el-table>
          <el-empty v-else :description="$t('home.noPending')" :image-size="70" />
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" style="margin-bottom:16px">
          <div class="card-title">{{ $t('home.quickActions') }}</div>
          <div class="quick">
            <div class="q" role="button" tabindex="0" @click="$router.push('/student/rooms')" @keyup.enter="$router.push('/student/rooms')"><span>🔍</span>{{ $t('home.qBook') }}</div>
            <div class="q" role="button" tabindex="0" @click="$router.push('/student/nearby')" @keyup.enter="$router.push('/student/nearby')"><span>📍</span>{{ $t('home.qNearby') }}</div>
            <div class="q" role="button" tabindex="0" @click="$router.push('/student/reservations')" @keyup.enter="$router.push('/student/reservations')"><span>📋</span>{{ $t('home.qMine') }}</div>
            <div class="q" role="button" tabindex="0" @click="$router.push('/student/ranking')" @keyup.enter="$router.push('/student/ranking')"><span>🏆</span>{{ $t('home.qRanking') }}</div>
          </div>
        </el-card>
        <el-card shadow="never">
          <div class="card-title">{{ $t('home.topRank') }}</div>
          <div v-for="(r, i) in topRank" :key="r.userId" class="rk" :class="{ me: r.userId === user.userInfo?.id }">
            <span class="rk-no">{{ trophy(i + 1) }}{{ i + 1 }}</span>
            <span class="rk-name">{{ r.realName }}{{ r.userId === user.userInfo?.id ? $t('home.me') : '' }}</span>
            <span class="rk-score">{{ $t('home.points', { n: r.creditScore }) }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '../../stores/user'
import { reservationApi, scoreApi, announcementApi } from '../../api'
import CountUp from '../../components/CountUp.vue'

const user = useUserStore()
const stat = reactive({ today: 0, inUse: 0, completed: 0 })
const upcoming = ref([])
const topRank = ref([])
const myRank = ref(null)
const announcements = ref([])

function trophy(r) { return r === 1 ? '🥇' : r === 2 ? '🥈' : r === 3 ? '🥉' : '' }

onMounted(async () => {
  await user.refreshProfile().catch(() => {})
  const list = await reservationApi.mine().catch(() => [])
  const today = new Date(); const t = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  stat.today = list.filter(r => r.date === t).length
  stat.inUse = list.filter(r => r.status === 'IN_USE').length
  stat.completed = list.filter(r => r.status === 'COMPLETED').length
  upcoming.value = list.filter(r => ['PENDING_SIGN_IN', 'IN_USE'].includes(r.status)).slice(0, 6)
  const rank = await scoreApi.ranking('week').catch(() => [])
  topRank.value = rank.slice(0, 5)
  const mine = rank.find(r => r.userId === user.userInfo?.id)
  myRank.value = mine ? mine.rank : null
  announcements.value = (await announcementApi.list().catch(() => [])).slice(0, 3)
})
</script>

<style scoped>
.hero { background: linear-gradient(135deg, #3b6cff, #8f5bff); color: #fff; border-radius: 16px; padding: 24px 28px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.hero-hi { font-size: 22px; font-weight: 800; }
.hero-sub { opacity: .9; margin-top: 6px; }
.hero-stats { display: flex; gap: 30px; }
.hero-stat { text-align: center; }
.hero-stat .v { font-size: 28px; font-weight: 800; }
.hero-stat .k { font-size: 12px; opacity: .9; }
.ov { display: flex; align-items: center; gap: 14px; }
.ov .ic { width: 46px; height: 46px; border-radius: 12px; display: grid; place-items: center; font-size: 22px; }
.ov-v { font-size: 24px; font-weight: 800; }
.ov-k { font-size: 12px; color: #8a93a6; }
.card-title { font-weight: 700; margin-bottom: 12px; }
.quick { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.q { background: #f7f8fc; border-radius: 12px; padding: 16px; text-align: center; cursor: pointer; font-weight: 600; transition: all .12s; }
.q span { display: block; font-size: 24px; margin-bottom: 6px; }
.q:hover { background: #eef2ff; color: #3b6cff; }
.rk { display: flex; align-items: center; gap: 10px; padding: 8px 4px; border-bottom: 1px dashed #eef0f5; }
.rk.me { background: #eef4ff; border-radius: 8px; }
.rk-no { font-weight: 700; width: 44px; color: #d98a00; }
.rk-name { flex: 1; }
.rk-score { color: #1f9d55; font-weight: 700; }
</style>
