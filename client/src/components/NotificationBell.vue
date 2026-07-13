<template>
  <span>
    <el-badge :value="unread" :hidden="unread === 0" :max="99">
      <el-button circle :icon="Bell" :aria-label="$t('noti.bell')" @click="openDrawer" />
    </el-badge>

    <el-drawer v-model="drawer" :title="$t('noti.title')" size="380px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;width:100%">
          <span style="font-weight:700">{{ $t('noti.title') }}</span>
          <el-button link type="primary" size="small" @click="readAll" :disabled="unread===0">{{ $t('noti.readAll') }}</el-button>
        </div>
      </template>

      <!-- 公告横幅（独立于通知列表，与 Home.vue 一致） -->
      <div v-if="announcements.length" style="margin-bottom:12px">
        <div style="font-size:12px;font-weight:700;color:#8a93a6;padding:0 4px 6px">📢 {{ $t('noti.announce') }}</div>
        <el-alert v-for="a in announcements" :key="a.id" :type="a.level === 'WARN' ? 'warning' : 'info'"
          :closable="false" show-icon style="margin-bottom:8px">
          <template #title>
            <span style="font-weight:700">{{ a.title }}</span>
            <span style="margin-left:8px;color:#8a93a6;font-size:12px">{{ fmt(a.createdTime) }}</span>
          </template>
          <div style="font-size:13px">{{ a.content }}</div>
        </el-alert>
      </div>

      <!-- 其他通知列表 -->
      <div v-if="!notifications.length" style="color:#8a93a6;text-align:center;margin-top:24px">{{ $t('noti.empty') }}</div>
      <div v-for="n in notifications" :key="n.id" class="noti" :class="{ unread: !n.readFlag }" @click="read(n)">
        <div class="noti-side">{{ typeIcon(n.type) }}</div>
        <div style="flex:1;min-width:0">
          <div class="noti-top">
            <b>{{ n.title }}</b>
            <span class="noti-time">{{ fmt(n.createdTime) }}</span>
          </div>
          <div class="noti-content">{{ n.content }}</div>
        </div>
      </div>
    </el-drawer>
  </span>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { ElNotification } from 'element-plus'
import { notificationApi, announcementApi } from '../api'
import { connectNotifications } from '../api/notificationStream'

const unread = ref(0)
const list = ref([])
const announcements = ref([])
const drawer = ref(false)
let stream = null

// 从通知列表中过滤出非公告类型
const notifications = computed(() => list.value.filter(n => n.type !== 'ANNOUNCEMENT'))

// 与后端 NotificationService 发出的 type 一一对应：[标签, 颜色, 图标]
const typeMap = {
  SCORE: ['积分', 'warning', '⭐'],
  BLACKLIST: ['黑名单', 'danger', '⛔'],
  WAITLIST: ['候补', 'success', '🔔'],
  GROUP: ['组队', 'primary', '👥'],
  REMINDER: ['提醒', 'warning', '⏰'],
  RESERVATION: ['预约', 'primary', '📋'],
  SYSTEM: ['系统', 'info', '🛎️']
}
const typeIcon = (t) => typeMap[t]?.[2] || '🔔'
const fmt = (t) => t ? String(t).replace('T', ' ').slice(5, 16) : ''

onMounted(async () => {
  await refresh()
  stream = connectNotifications({
    notification: (p) => {
      unread.value = p.unread ?? unread.value + 1
      ElNotification({ title: p.title, message: p.content, type: p.type === 'BLACKLIST' ? 'error' : 'success', position: 'bottom-right', duration: 5000 })
      if (drawer.value) loadList()
    }
  })
})
onBeforeUnmount(() => { if (stream) stream.close() })

async function refresh() {
  unread.value = (await notificationApi.unread()).unread
  announcements.value = (await announcementApi.list().catch(() => [])).slice(0, 5)
}
async function loadList() { list.value = await notificationApi.list() }
async function openDrawer() { drawer.value = true; await loadList() }
async function read(n) {
  if (n.readFlag) return
  await notificationApi.read(n.id); n.readFlag = 1; await refresh()
}
async function readAll() { await notificationApi.readAll(); list.value.forEach(n => n.readFlag = 1); await refresh() }
</script>

<style scoped>
.noti { padding: 10px 12px; border-radius: 10px; margin-bottom: 8px; cursor: pointer; display:flex; gap:10px; align-items:flex-start; background:#f7f8fc; }
.noti.unread { background: #eef4ff; }
.noti-side { font-size:20px; width:28px; text-align:center; line-height:1.4; }
.noti-top { display: flex; align-items: center; gap: 8px; }
.noti-top b { font-size: 13px; }
.noti-time { margin-left: auto; font-size: 11px; color: #a2abbd; white-space:nowrap; }
.noti-content { font-size: 12px; color: #5a6172; margin-top: 4px; line-height: 1.5; }
</style>
