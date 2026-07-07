<template>
  <div class="page">
    <div class="page-title">积分排行榜</div>
    <div class="page-sub">守约加分（签退 +2）· 临近取消 -1 · 超时未签到 -3</div>
    <el-card shadow="never">
      <el-table :data="list" style="width:100%">
        <el-table-column label="名次" width="90">
          <template #default="{ row }">
            <span :style="{fontWeight:700, color: medal(row.rank)}">{{ trophy(row.rank) }} {{ row.rank }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="学生" />
        <el-table-column prop="creditScore" label="积分" width="120" sortable />
        <el-table-column prop="noShowCount" label="爽约次数" width="120" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { scoreApi } from '../../api'
const list = ref([])
onMounted(async () => { list.value = await scoreApi.ranking('week') })
function trophy(r) { return r === 1 ? '🥇' : r === 2 ? '🥈' : r === 3 ? '🥉' : '' }
function medal(r) { return r <= 3 ? '#d98a00' : '#1f2430' }
</script>
