<template>
  <div class="page">
    <div class="page-title">黑名单管理</div>
    <div class="page-sub">爽约达阈值自动进入黑名单，限制预约但不限制登录/查看</div>
    <el-card shadow="never">
      <el-table :data="list" style="width:100%">
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="reason" label="原因" />
        <el-table-column label="生效" width="180"><template #default="{ row }">{{ fmt(row.startTime) }}</template></el-table-column>
        <el-table-column label="到期" width="180"><template #default="{ row }">{{ fmt(row.endTime) }}</template></el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.active?'danger':'info'">{{ row.active?'生效中':'已解除' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="row.active" size="small" type="primary" @click="release(row)">解除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!list.length" description="暂无黑名单记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { blacklistApi } from '../../api'
const list = ref([])
onMounted(load)
async function load() { list.value = await blacklistApi.list() }
async function release(row) {
  await blacklistApi.release(row.id)
  ElMessage.success('已解除')
  await load()
}
function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : '' }
</script>
