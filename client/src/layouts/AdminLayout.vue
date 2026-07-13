<template>
  <el-container style="height: 100vh">
    <div v-if="isMobile && drawerOpen" class="nav-backdrop" @click="drawerOpen = false"></div>
    <el-aside width="210px" class="app-aside" :class="{ 'is-mobile': isMobile, open: drawerOpen }" style="background:#1a2233">
      <div style="padding:18px 16px">
        <div class="brand"><span class="logo">🛠️</span> SeatWise</div>
      </div>
      <el-menu :default-active="active" router background-color="#1a2233" text-color="#c7cfe2"
               active-text-color="#fff" role="navigation" :aria-label="$t('app.adminConsole')" @select="onSelect">
        <el-menu-item index="/admin/dashboard"><el-icon><Odometer /></el-icon><span>{{ $t('nav.dashboard') }}</span></el-menu-item>
        <el-menu-item index="/admin/rooms"><el-icon><OfficeBuilding /></el-icon><span>{{ $t('nav.rooms') }}</span></el-menu-item>
        <el-menu-item index="/admin/spacetime"><el-icon><Compass /></el-icon><span>{{ $t('nav.spacetimeAdmin') }}</span></el-menu-item>
        <el-menu-item index="/admin/students"><el-icon><User /></el-icon><span>{{ $t('nav.students') }}</span></el-menu-item>
        <el-menu-item index="/admin/reports"><el-icon><DataAnalysis /></el-icon><span>{{ $t('nav.reports') }}</span></el-menu-item>
        <el-menu-item index="/admin/announcements"><el-icon><Bell /></el-icon><span>{{ $t('nav.announcements') }}</span></el-menu-item>
        <el-menu-item index="/admin/locations"><el-icon><MapLocation /></el-icon><span>{{ $t('nav.locations') }}</span></el-menu-item>
        <el-menu-item index="/admin/ranking"><el-icon><Trophy /></el-icon><span>{{ $t('nav.ranking') }}</span></el-menu-item>
        <el-menu-item index="/admin/blacklist"><el-icon><Warning /></el-icon><span>{{ $t('nav.blacklist') }}</span></el-menu-item>
        <el-menu-item v-if="isPrimary" index="/admin/admins"><el-icon><UserFilled /></el-icon><span>{{ $t('nav.admins') }}</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header" style="background:#fff;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #eef0f5;gap:8px">
        <div style="display:flex;align-items:center;gap:10px;min-width:0">
          <el-button v-if="isMobile" circle text :icon="Expand" :aria-label="$t('common.menu')" @click="drawerOpen = true" />
          <div class="header-title" style="font-weight:600">{{ isMobile ? 'SeatWise' : $t('app.adminConsole') }}</div>
        </div>
        <div style="display:flex;align-items:center;gap:10px">
          <LanguageSwitcher />
          <el-tooltip :content="theme === 'dark' ? $t('common.toLight') : $t('common.toDark')" placement="bottom">
            <el-button circle :icon="theme === 'dark' ? Sunny : Moon" :aria-label="theme === 'dark' ? $t('common.toLight') : $t('common.toDark')" @click="toggleTheme" />
          </el-tooltip>
          <span v-if="!isMobile">{{ user.userInfo?.realName }}（{{ isPrimary ? $t('common.primaryAdmin') : $t('common.subAdmin') }}）</span>
          <el-button link type="primary" @click="logout">{{ $t('common.logout') }}</el-button>
        </div>
      </el-header>
      <el-main id="main-content" style="background:transparent">
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
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Sunny, Moon, Trophy, Expand } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { theme, toggleTheme } from '../utils/theme'
import LanguageSwitcher from '../components/LanguageSwitcher.vue'
import { useMobile } from '../utils/useMobile'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const active = computed(() => '/admin/' + (route.path.split('/')[2] || 'rooms'))
const isPrimary = computed(() => (user.role || user.userInfo?.role) === 'ADMIN')
const { isMobile } = useMobile()
const drawerOpen = ref(false)

function onSelect() { if (isMobile.value) drawerOpen.value = false }

function logout() {
  user.logout()
  router.push('/login')
}
</script>
