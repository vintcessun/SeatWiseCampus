<template>
  <span>
    <el-badge :value="unread" :hidden="unread === 0" :max="99">
      <el-button circle :icon="Bell" @click="openDrawer" />
    </el-badge>

    <el-drawer v-model="drawer" title="站内通知" size="380px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;width:100%">
          <span style="font-weight:700">站内通知</span>
          <el-button link type="primary" size="small" @click="readAll" :disabled="unread===0">全部已读</el-button>
        </div>
      </template>
      <div v-if="!list.length" style="color:#8a93a6;text-align:center;margin-top:40px">暂无通知</div>
      <div v-for="n in list" :key="n.id" class="noti" :class="{ unread: !n.readFlag }" @click="read(n)">
        <div class="noti-top">
          <el-tag size="small" :type="typeColor(n.type)" effect="plain">{{ typeIcon(n.type) }} {{ typeLabel(n.type) }}</el-tag>
          <b>{{ n.title }}</b>
          <span class="noti-time">{{ fmt(n.createdTime) }}</span>
        </div>
        <div class="noti-content">{{ n.content }}</div>
      </div>
    </el-drawer>
  </span>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { ElNotification } from 'element-plus'
import { notificationApi } from '../api'
import { connectNotifications } from '../api/notificationStream'

const unread = ref(0)
const list = ref([])
const drawer = ref(false)
let stream = null

// 与后端 NotificationService 发出的 type 一一对应：[标签, 颜色, 图标]
const typeMap = {
  SCORE: ['积分', 'warning', '⭐'],
  BLACKLIST: ['黑名单', 'danger', '⛔'],
  WAITLIST: ['候补', 'success', '🔔'],
  GROUP: ['组队', 'primary', '👥'],
  ANNOUNCEMENT: ['公告', 'info', '📢'],
  REMINDER: ['提醒', 'warning', '⏰'],
  RESERVATION: ['预约', 'primary', '📋'],
  SYSTEM: ['系统', 'info', '🛎️']
}
const typeLabel = (t) => typeMap[t]?.[0] || '通知'
const typeColor = (t) => typeMap[t]?.[1] || 'info'
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

async function refresh() { unread.value = (await notificationApi.unread()).unread }
async function loadList() { list.value = await notificationApi.list() }
async function openDrawer() { drawer.value = true; await loadList() }
async function read(n) {
  if (n.readFlag) return
  await notificationApi.read(n.id); n.readFlag = 1; await refresh()
}
async function readAll() { await notificationApi.readAll(); list.value.forEach(n => n.readFlag = 1); await refresh() }
</script>

<style scoped>
.noti { padding: 10px 12px; border-radius: 10px; margin-bottom: 8px; background: #f7f8fc; cursor: pointer; border: 1px solid transparent; }
.noti.unread { background: #eef4ff; border-color: #d6e4ff; }
.noti-top { display: flex; align-items: center; gap: 8px; }
.noti-top b { font-size: 13px; }
.noti-time { margin-left: auto; font-size: 11px; color: #a2abbd; }
.noti-content { font-size: 12px; color: #5a6172; margin-top: 4px; line-height: 1.5; }
</style>
