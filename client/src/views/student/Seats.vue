<template>
  <div class="page">
    <div style="display:flex;justify-content:space-between;align-items:center">
      <div>
        <div class="page-title">{{ board.roomName || '选座' }}</div>
        <div class="page-sub">
          实时看板：
          <el-tag size="small" :type="sseOk ? 'success' : 'info'" effect="plain">
            {{ sseOk ? '实时连接中' : '连接中…' }}
          </el-tag>
          <span style="margin-left:8px;color:#b8860b">🔒 点座后为你保留 {{ holdSeconds }} 秒，其他人会看到"选择中"</span>
        </div>
      </div>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <el-card shadow="never" style="margin-bottom:16px">
      <div style="display:flex;gap:12px;flex-wrap:wrap;align-items:center">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="onWindowChange" />
        <el-time-select v-model="start" start="08:00" end="21:30" step="00:30" placeholder="开始" style="width:130px" @change="onWindowChange" />
        <el-time-select v-model="end" start="08:30" end="22:00" step="00:30" placeholder="结束" style="width:130px" @change="reload" />
        <el-button :icon="Refresh" @click="reload">刷新座位</el-button>
        <span style="color:#8a93a6;font-size:13px">点击绿色空闲座位进行预约</span>
        <el-button v-if="freeCount === 0" type="warning" plain :icon="Bell" @click="joinWaitlist">
          全部占满，加入候补队列
        </el-button>
        <el-tag v-else type="success" effect="plain" size="small">当前 {{ freeCount }} 个空位</el-tag>
        <el-divider direction="vertical" />
        <el-switch v-model="groupMode" active-text="组队相邻预约" @change="onGroupToggle" />
      </div>
    </el-card>

    <el-card v-if="groupMode" shadow="never" style="margin-bottom:16px;border:1px solid #8f5bff33">
      <div style="font-weight:600;margin-bottom:8px">👥 组队相邻预约<span style="color:#8a93a6;font-weight:400;font-size:13px">（点座位图选择<strong>同一排连续</strong>的空位，为每位成员填写用户名；全部成功或整体取消）</span></div>
      <el-empty v-if="!groupSeats.length" description="请在下方座位图选择相邻空位" :image-size="50" />
      <div v-for="(g, i) in groupSeats" :key="g.seatId" style="display:flex;align-items:center;gap:10px;margin-bottom:8px">
        <el-tag type="primary" effect="plain">座位 {{ g.seatNo }}</el-tag>
        <el-input v-model="g.username" placeholder="成员用户名" style="width:180px" :prefix-icon="User" />
        <span v-if="i === 0" style="color:#8a93a6;font-size:12px">默认发起人</span>
        <el-button link type="danger" @click="removeGroupSeat(g.seatId)">移除</el-button>
      </div>
      <el-button type="primary" :loading="submittingGroup" :disabled="!groupSeats.length" @click="submitGroup">
        提交组队预约（{{ groupSeats.length }} 座）
      </el-button>
      <el-button v-if="groupSeats.length" @click="groupSeats = []">清空</el-button>
    </el-card>

    <el-card shadow="never">
      <SeatGrid :cells="board.seats || []" :cols="board.cols || 8" :now-ms="nowMs" :selected-ids="selectedIds" selectable @select="onSelect" />
    </el-card>

    <el-dialog v-model="dialog" title="确认预约" width="380px" @close="onDialogClose">
      <p>自习室：{{ board.roomName }}</p>
      <p>座位：<b>{{ picked?.seatNo }}</b></p>
      <p>日期：{{ date }}</p>
      <p>时段：{{ start }} - {{ end }}</p>
      <el-alert type="warning" :closable="false" show-icon
        :title="`该座位已为你临时保留，剩余 ${dialogRemain} 秒，请尽快确认`" style="margin-top:8px" />
      <template #footer>
        <el-button @click="dialog=false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="dialogRemain<=0" @click="submit">确认预约</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="altDialog" title="🤖 座位刚被抢走，为你找到替代方案" width="420px">
      <p style="color:#8a93a6;margin:0 0 12px">同时段可用座位，点击一键改约：</p>
      <div v-for="a in alts" :key="a.roomId + '-' + a.seatId" class="alt" @click="chooseAlt(a)">
        <div>
          <b>{{ a.roomName }} · {{ a.seatNo }}</b>
          <el-tag size="small" :type="a.sameRoom ? 'success' : 'info'" effect="plain" style="margin-left:8px">
            {{ a.sameRoom ? '同房间相邻' : '同校区其它' }}
          </el-tag>
        </div>
        <div style="font-size:12px;color:#8a93a6;margin-top:2px">{{ a.reason }}</div>
        <div style="font-size:12px;color:#3b6cff;margin-top:4px">{{ a.sameRoom ? '一键改约 →' : '前往该自习室 →' }}</div>
      </div>
      <el-empty v-if="!alts.length" description="附近暂无替代空位" :image-size="60" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh, Bell, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import SeatGrid from '../../components/SeatGrid.vue'
import { boardApi, reservationApi, holdApi, nearbyApi, waitlistApi } from '../../api'
import { connectBoardStream } from '../../api/boardStream'
import { todayLocal } from '../../utils/date'
import { useUserStore } from '../../stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const roomId = route.params.roomId
const altDialog = ref(false)
const alts = ref([])
const date = ref(todayLocal())
const start = ref('14:00')
const end = ref('16:00')
const board = reactive({ seats: [], cols: 8, roomName: '' })
const dialog = ref(false)
const picked = ref(null)
const pickedExpireAt = ref(0)
const submitting = ref(false)
const confirmed = ref(false)
const sseOk = ref(false)
const nowMs = ref(Date.now())
const holdSeconds = 90
let stream = null
let ticker = null

const myId = computed(() => user.userInfo?.id)
const dialogRemain = computed(() => Math.max(0, Math.ceil((pickedExpireAt.value - nowMs.value) / 1000)))
const freeCount = computed(() => (board.seats || []).filter(s => s.status === 'FREE').length)

async function joinWaitlist() {
  if (start.value >= end.value) { ElMessage.warning('开始时间必须早于结束时间'); return }
  try {
    await waitlistApi.join({ roomId: Number(roomId), date: date.value, startTime: start.value, endTime: end.value })
    ElMessage.success('已加入候补队列！有人释放座位时会第一时间通知你')
  } catch (e) {
    ElMessage.warning(e?.message || '加入候补失败')
  }
}

// ===== 组队相邻预约 =====
const groupMode = ref(false)
const groupSeats = ref([]) // [{ seatId, seatNo, username }]
const submittingGroup = ref(false)
const selectedIds = computed(() => groupSeats.value.map(s => s.seatId))

function onGroupToggle(v) { if (!v) groupSeats.value = [] }
function removeGroupSeat(seatId) {
  const i = groupSeats.value.findIndex(s => s.seatId === seatId)
  if (i >= 0) groupSeats.value.splice(i, 1)
}
function toggleGroupSeat(cell) {
  const i = groupSeats.value.findIndex(s => s.seatId === cell.seatId)
  if (i >= 0) { groupSeats.value.splice(i, 1); return }
  if (groupSeats.value.length >= 6) { ElMessage.warning('单次组队最多 6 个相邻座位'); return }
  const defaultName = groupSeats.value.length === 0 ? (user.userInfo?.username || '') : ''
  groupSeats.value.push({ seatId: cell.seatId, seatNo: cell.seatNo, username: defaultName })
}
async function submitGroup() {
  if (start.value >= end.value) { ElMessage.warning('开始时间必须早于结束时间'); return }
  if (new Date(`${date.value}T${start.value}:00`).getTime() <= Date.now()) { ElMessage.warning('预约开始时间需晚于当前时间'); return }
  if (!groupSeats.value.length) { ElMessage.warning('请先在座位图选择相邻空位'); return }
  if (groupSeats.value.some(s => !s.username.trim())) { ElMessage.warning('请为每个座位填写成员用户名'); return }
  submittingGroup.value = true
  try {
    const res = await reservationApi.group({
      roomId: Number(roomId), date: date.value, startTime: start.value, endTime: end.value,
      members: groupSeats.value.map(s => ({ seatId: s.seatId, username: s.username.trim() }))
    })
    ElMessage.success(`组队预约成功！已为小组原子锁定 ${res.length} 个相邻座位`)
    groupSeats.value = []
    await reload()
  } catch (e) {
    ElMessage.error(e?.message || '组队预约失败（整单已取消）')
    await reload()
  } finally { submittingGroup.value = false }
}

onMounted(() => {
  reload(); openStream()
  ticker = setInterval(() => { nowMs.value = Date.now() }, 1000)
})
onBeforeUnmount(() => { if (stream) stream.close(); if (ticker) clearInterval(ticker); releaseCurrent() })

async function reload() {
  const data = await boardApi.snapshot(roomId, { date: date.value, start: start.value, end: end.value })
  Object.assign(board, data)
}
function onWindowChange() { openStream(); reload() }

function openStream() {
  if (stream) stream.close()
  stream = connectBoardStream({ roomId, date: date.value }, {
    onOpen: () => (sseOk.value = true),
    onError: () => (sseOk.value = false),
    seat_reserved: (p) => setSeat(p.seatId, { status: 'RESERVED', heldBy: null, holdExpireAt: null }),
    seat_released: (p) => setSeat(p.seatId, { status: 'FREE', heldBy: null, holdExpireAt: null }),
    seat_in_use: (p) => setSeat(p.seatId, { status: 'USING' }),
    seat_disabled: (p) => setSeat(p.seatId, { status: 'DISABLED' }),
    seat_hold: (p) => setSeat(p.seatId, { status: 'HELD', heldBy: p.byUserId, holdExpireAt: p.expireAt, mine: p.byUserId === myId.value }),
    hold_released: (p) => setSeat(p.seatId, { status: 'FREE', heldBy: null, holdExpireAt: null, mine: false })
  })
}

function setSeat(seatId, patch) {
  const seat = (board.seats || []).find(s => s.seatId === seatId)
  if (seat) Object.assign(seat, patch)
}

async function onSelect(cell) {
  if (groupMode.value) { toggleGroupSeat(cell); return }
  if (start.value >= end.value) { ElMessage.warning('开始时间必须早于结束时间'); return }
  if (new Date(`${date.value}T${start.value}:00`).getTime() <= Date.now()) {
    ElMessage.warning('预约开始时间需晚于当前时间'); return
  }
  try {
    const res = await holdApi.hold({ roomId: Number(roomId), seatId: cell.seatId, date: date.value, startTime: start.value, endTime: end.value })
    picked.value = cell
    pickedExpireAt.value = res.expireAt
    confirmed.value = false
    dialog.value = true
  } catch (e) {
    await reload() // 已被他人锁/占用
  }
}

async function submit() { await reserveSeat(picked.value.seatId) }

async function reserveSeat(seatId) {
  submitting.value = true
  try {
    await reservationApi.create({
      roomId: Number(roomId), seatId,
      date: date.value, startTime: start.value, endTime: end.value
    })
    ElMessage.success('预约成功！座位已锁定，请按时签到')
    confirmed.value = true
    dialog.value = false
    altDialog.value = false
    await reload()
  } catch (e) {
    await reload()
    // 抢座失败 → 智能替代方案
    if (e?.code === 'SEAT_ALREADY_RESERVED') {
      const list = await nearbyApi.alternatives({ roomId: Number(roomId), date: date.value, start: start.value, end: end.value, excludeSeatId: seatId }).catch(() => [])
      if (list && list.length) { alts.value = list; dialog.value = false; altDialog.value = true }
    }
  } finally {
    submitting.value = false
  }
}

async function chooseAlt(a) {
  if (a.sameRoom) {
    picked.value = { seatId: a.seatId, seatNo: a.seatNo }
    confirmed.value = false
    await reserveSeat(a.seatId)
  } else {
    altDialog.value = false
    releaseCurrent()
    router.push(`/student/rooms/${a.roomId}/seats`)
  }
}

// 关闭对话框但未确认 → 释放临时锁
function onDialogClose() {
  if (!confirmed.value) releaseCurrent()
}
function releaseCurrent() {
  if (picked.value && !confirmed.value) {
    holdApi.release({ roomId: Number(roomId), seatId: picked.value.seatId, date: date.value }).catch(() => {})
    picked.value = null
  }
}
</script>
