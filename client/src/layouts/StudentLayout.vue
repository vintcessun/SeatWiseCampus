<template>
  <el-container style="height: 100vh">
    <el-aside width="210px" style="background:#1f2740">
      <div style="padding:18px 16px">
        <div class="brand"><span class="logo">🎓</span> SeatWise</div>
      </div>
      <el-menu :default-active="active" router background-color="#1f2740" text-color="#c7cfe2"
               active-text-color="#fff">
        <el-menu-item index="/student/home"><el-icon><HomeFilled /></el-icon><span>首页概览</span></el-menu-item>
        <el-menu-item index="/student/rooms"><el-icon><Search /></el-icon><span>选座预约</span></el-menu-item>
        <el-menu-item index="/student/reservations"><el-icon><Tickets /></el-icon><span>我的预约</span></el-menu-item>
        <el-menu-item index="/student/waitlist"><el-icon><BellFilled /></el-icon><span>我的候补</span></el-menu-item>
        <el-menu-item index="/student/report"><el-icon><DataLine /></el-icon><span>自习报告</span></el-menu-item>
        <el-menu-item index="/student/nearby"><el-icon><LocationInformation /></el-icon><span>附近空位</span></el-menu-item>
        <el-menu-item index="/student/ranking"><el-icon><Trophy /></el-icon><span>积分排行</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background:#fff;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #eef0f5">
        <div style="font-weight:600">智能校园自习室预约管理平台</div>
        <div style="display:flex;align-items:center;gap:14px">
          <NotificationBell />
          <el-tag type="success" effect="plain">积分 {{ user.userInfo?.creditScore ?? 0 }}</el-tag>
          <span>{{ user.userInfo?.realName }}（学生）</span>
          <el-button link type="primary" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main style="background:transparent">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
    <AiAssistant />
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import AiAssistant from '../components/AiAssistant.vue'
import NotificationBell from '../components/NotificationBell.vue'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const active = computed(() => route.path)

onMounted(() => { user.refreshProfile().catch(() => {}) })

function logout() {
  user.logout()
  router.push('/login')
}
</script>
