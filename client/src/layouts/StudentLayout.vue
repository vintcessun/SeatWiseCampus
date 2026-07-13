<template>
  <el-container style="height: 100vh">
    <div v-if="isMobile && drawerOpen" class="nav-backdrop" @click="drawerOpen = false"></div>
    <el-aside width="210px" class="app-aside" :class="{ 'is-mobile': isMobile, open: drawerOpen }" style="background:#1f2740">
      <div style="padding:18px 16px">
        <div class="brand"><span class="logo">🎓</span> SeatWise</div>
      </div>
      <el-menu :default-active="active" router background-color="#1f2740" text-color="#c7cfe2"
               active-text-color="#fff" role="navigation" :aria-label="$t('app.brand')" @select="onSelect">
        <el-menu-item index="/student/home"><el-icon><HomeFilled /></el-icon><span>{{ $t('nav.home') }}</span></el-menu-item>
        <el-menu-item index="/student/rooms"><el-icon><Search /></el-icon><span>{{ $t('nav.booking') }}</span></el-menu-item>
        <el-menu-item index="/student/spacetime"><el-icon><Compass /></el-icon><span>{{ $t('nav.spacetime') }}</span></el-menu-item>
        <el-menu-item index="/student/reservations"><el-icon><Tickets /></el-icon><span>{{ $t('nav.myReservations') }}</span></el-menu-item>
        <el-menu-item index="/student/waitlist"><el-icon><BellFilled /></el-icon><span>{{ $t('nav.myWaitlist') }}</span></el-menu-item>
        <el-menu-item index="/student/report"><el-icon><DataLine /></el-icon><span>{{ $t('nav.studyReport') }}</span></el-menu-item>
        <el-menu-item index="/student/pomodoro"><el-icon><Timer /></el-icon><span>{{ $t('nav.pomodoro') }}</span></el-menu-item>
        <el-menu-item index="/student/nearby"><el-icon><LocationInformation /></el-icon><span>{{ $t('nav.nearby') }}</span></el-menu-item>
        <el-menu-item index="/student/ranking"><el-icon><Trophy /></el-icon><span>{{ $t('nav.ranking') }}</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header" style="background:#fff;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #eef0f5;gap:8px">
        <div style="display:flex;align-items:center;gap:10px;min-width:0">
          <el-button v-if="isMobile" circle text :icon="Expand" :aria-label="$t('common.menu')" @click="drawerOpen = true" />
          <div class="header-title" style="font-weight:600">{{ isMobile ? 'SeatWise' : $t('app.title') }}</div>
        </div>
        <div style="display:flex;align-items:center;gap:10px">
          <LanguageSwitcher />
          <el-tooltip :content="theme === 'dark' ? $t('common.toLight') : $t('common.toDark')" placement="bottom">
            <el-button circle :icon="theme === 'dark' ? Sunny : Moon" :aria-label="theme === 'dark' ? $t('common.toLight') : $t('common.toDark')" @click="toggleTheme" />
          </el-tooltip>
          <NotificationBell />
          <el-tag v-if="!isMobile" type="success" effect="plain">{{ $t('common.credit') }} {{ user.userInfo?.creditScore ?? 0 }}</el-tag>
          <span v-if="!isMobile">{{ user.userInfo?.realName }}（{{ $t('common.student') }}）</span>
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
    <AiAssistant />
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Sunny, Moon, Expand } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import AiAssistant from '../components/AiAssistant.vue'
import NotificationBell from '../components/NotificationBell.vue'
import LanguageSwitcher from '../components/LanguageSwitcher.vue'
import { theme, toggleTheme } from '../utils/theme'
import { useMobile } from '../utils/useMobile'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const active = computed(() => route.path)
const { isMobile } = useMobile()
const drawerOpen = ref(false)

function onSelect() { if (isMobile.value) drawerOpen.value = false }

onMounted(() => { user.refreshProfile().catch(() => {}) })

function logout() {
  user.logout()
  router.push('/login')
}
</script>
