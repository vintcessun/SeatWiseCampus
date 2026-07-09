<template>
  <div>
    <!-- 悬浮按钮 -->
    <div class="ai-fab" @click="open = !open" :class="{ active: open }">
      <span v-if="!open">🤖</span><span v-else>✕</span>
    </div>

    <!-- 面板 -->
    <transition name="ai-slide">
      <div v-if="open" class="ai-panel">
        <div class="ai-head">
          <div>
            <div class="ai-title">AI 智能选座助手</div>
            <div class="ai-sub">说出你的需求，帮你找到最合适的座位</div>
          </div>
          <el-tag size="small" :type="lastSource==='llm'?'success':'info'" effect="plain">
            {{ lastSource==='llm' ? '大模型' : '规则引擎' }}
          </el-tag>
        </div>

        <div class="ai-body" ref="bodyEl">
          <div class="ai-msg bot">你好！我可以按时间、时长、靠窗/插座/安静等偏好帮你推荐座位。试试下面的例子👇</div>
          <div class="ai-examples">
            <span v-for="ex in examples" :key="ex" class="ai-chip" @click="send(ex)">{{ ex }}</span>
          </div>

          <template v-for="(m, i) in history" :key="i">
            <div class="ai-msg user">{{ m.q }}</div>
            <div class="ai-msg bot">
              <div>{{ m.reply }}</div>
              <div v-if="m.recs && m.recs.length" class="ai-recs">
                <div v-for="(r, idx) in m.recs" :key="r.seatId" class="ai-rec" @click="go(r)">
                  <div class="ai-rec-top">
                    <span class="ai-rec-rank">{{ idx===0 ? '⭐ 首选' : '备选'+idx }}</span>
                    <b>{{ r.roomName }} · {{ r.seatNo }}</b>
                  </div>
                  <div class="ai-rec-tags">
                    <span v-for="t in r.tags" :key="t" class="ai-tag">{{ tagCn(t) }}</span>
                  </div>
                  <div class="ai-rec-reason">{{ r.reasons.join('，') }}</div>
                  <div class="ai-rec-go">前往预约 →</div>
                </div>
              </div>
            </div>
          </template>
          <div v-if="loading" class="ai-msg bot">正在思考…</div>
        </div>

        <div class="ai-input">
          <el-input v-model="text" placeholder="例如：明天上午找个安静靠窗的位置坐3小时"
                    @keyup.enter="send()" :disabled="loading" />
          <el-button type="primary" :loading="loading" @click="send()">发送</el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { aiApi } from '../api'

const router = useRouter()
const open = ref(false)
const text = ref('')
const loading = ref(false)
const history = ref([])
const lastSource = ref('rule')
const bodyEl = ref(null)
const examples = [
  '下午2点安静靠窗坐两小时',
  '找个有插座、人少的位置',
  '图书馆里能连续坐3小时的座位'
]
const tagMap = { window: '靠窗', power: '有插座', quiet: '安静区', discuss: '讨论区', near_door: '靠门' }
const tagCn = (t) => tagMap[t] || t

async function send(preset) {
  const q = (preset ?? text.value).trim()
  if (!q || loading.value) return
  text.value = ''
  loading.value = true
  try {
    const data = await aiApi.assistant({ message: q })
    lastSource.value = data.source
    history.value.push({ q, reply: data.reply, recs: data.recommendations || [] })
  } catch (e) {
    history.value.push({ q, reply: '抱歉，暂时无法给出推荐，请稍后再试。', recs: [] })
  } finally {
    loading.value = false
    await nextTick()
    if (bodyEl.value) bodyEl.value.scrollTop = bodyEl.value.scrollHeight
  }
}
function go(r) {
  open.value = false
  router.push(`/student/rooms/${r.roomId}/seats`)
}
</script>

<style scoped>
.ai-fab {
  position: fixed; right: 26px; bottom: 26px; width: 56px; height: 56px; border-radius: 50%;
  background: linear-gradient(135deg, #5b8cff, #8f5bff); color: #fff; font-size: 24px;
  display: grid; place-items: center; cursor: pointer; z-index: 2000;
  box-shadow: 0 8px 24px rgba(91, 140, 255, .45); transition: transform .15s;
}
.ai-fab:hover { transform: scale(1.08); }
.ai-fab.active { background: #6b7280; }
.ai-panel {
  position: fixed; right: 26px; bottom: 96px; width: 380px; max-height: 70vh;
  background: #fff; border-radius: 16px; z-index: 2000; display: flex; flex-direction: column;
  box-shadow: 0 16px 48px rgba(0,0,0,.18); overflow: hidden;
}
.ai-head { padding: 14px 16px; background: linear-gradient(135deg, #3b6cff, #8f5bff); color: #fff;
  display: flex; justify-content: space-between; align-items: center; }
.ai-title { font-weight: 700; font-size: 15px; }
.ai-sub { font-size: 11px; opacity: .85; margin-top: 2px; }
.ai-body { flex: 1; overflow-y: auto; padding: 14px; background: #f7f8fc; }
.ai-msg { padding: 9px 12px; border-radius: 12px; margin-bottom: 10px; font-size: 13px; line-height: 1.6; max-width: 92%; }
.ai-msg.bot { background: #fff; border: 1px solid #eef0f5; }
.ai-msg.user { background: #3b6cff; color: #fff; margin-left: auto; }
.ai-examples { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 10px; }
.ai-chip { background: #eef2ff; color: #3b6cff; font-size: 12px; padding: 4px 10px; border-radius: 12px; cursor: pointer; }
.ai-chip:hover { background: #e0e7ff; }
.ai-recs { margin-top: 8px; display: flex; flex-direction: column; gap: 8px; }
.ai-rec { border: 1px solid #e6e9f2; border-radius: 10px; padding: 8px 10px; cursor: pointer; background: #fff; transition: all .12s; }
.ai-rec:hover { border-color: #3b6cff; box-shadow: 0 2px 10px rgba(59,108,255,.12); }
.ai-rec-top { display: flex; align-items: center; gap: 8px; }
.ai-rec-rank { font-size: 11px; color: #d98a00; font-weight: 700; }
.ai-rec-tags { display: flex; flex-wrap: wrap; gap: 4px; margin: 5px 0; }
.ai-tag { font-size: 11px; background: #e3f6e9; color: #1f9d55; padding: 1px 7px; border-radius: 8px; }
.ai-rec-reason { font-size: 12px; color: #8a93a6; }
.ai-rec-go { font-size: 12px; color: #3b6cff; margin-top: 4px; }
.ai-input { padding: 10px; display: flex; gap: 8px; border-top: 1px solid #eef0f5; }
.ai-slide-enter-active, .ai-slide-leave-active { transition: all .18s; }
.ai-slide-enter-from, .ai-slide-leave-to { opacity: 0; transform: translateY(12px); }
</style>
