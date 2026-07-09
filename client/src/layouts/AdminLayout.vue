<template>
  <el-container style="height: 100vh">
    <el-aside width="210px" style="background:#1a2233">
      <div style="padding:18px 16px">
        <div class="brand"><span class="logo">🛠️</span> SeatWise</div>
      </div>
      <el-menu :default-active="active" router background-color="#1a2233" text-color="#c7cfe2"
               active-text-color="#fff">
        <el-menu-item index="/admin/dashboard"><el-icon><Odometer /></el-icon><span>概览首页</span></el-menu-item>
        <el-menu-item index="/admin/rooms"><el-icon><OfficeBuilding /></el-icon><span>自习室与座位</span></el-menu-item>
        <el-menu-item index="/admin/students"><el-icon><User /></el-icon><span>学生预约追踪</span></el-menu-item>
        <el-menu-item index="/admin/reports"><el-icon><DataAnalysis /></el-icon><span>数据报表</span></el-menu-item>
        <el-menu-item index="/admin/announcements"><el-icon><Bell /></el-icon><span>公告管理</span></el-menu-item>
        <el-menu-item index="/admin/blacklist"><el-icon><Warning /></el-icon><span>黑名单管理</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background:#fff;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #eef0f5">
        <div style="font-weight:600">管理控制台</div>
        <div style="display:flex;align-items:center;gap:14px">
          <span>{{ user.userInfo?.realName }}（管理员）</span>
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
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const active = computed(() => '/admin/' + (route.path.split('/')[2] || 'rooms'))

function logout() {
  user.logout()
  router.push('/login')
}
</script>
