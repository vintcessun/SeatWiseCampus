<template>
  <div class="page">
    <div style="display:flex;justify-content:space-between;align-items:center">
      <div>
        <div class="page-title">公告管理</div>
        <div class="page-sub">发布系统公告，学生端首页横幅展示；可选发布时一并推送站内通知</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">发布公告</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="list" style="width:100%">
        <el-table-column label="级别" width="90">
          <template #default="{ row }">
            <el-tag :type="row.level === 'WARN' ? 'warning' : 'info'" effect="plain">
              {{ row.level === 'WARN' ? '提醒' : '通知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" width="220" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? '生效中' : '已下线' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="120">
          <template #default="{ row }">{{ (row.createdTime || '').slice(0, 10) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="toggle(row)">{{ row.active ? '下线' : '上线' }}</el-button>
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!list.length" description="暂无公告，点击右上角发布" />
    </el-card>

    <el-dialog v-model="dialog" :title="form.id ? '编辑公告' : '发布公告'" width="480px">
      <el-form label-width="80px">
        <el-form-item label="标题"><el-input v-model="form.title" maxlength="128" show-word-limit /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="4" maxlength="1024" show-word-limit /></el-form-item>
        <el-form-item label="级别">
          <el-radio-group v-model="form.level">
            <el-radio label="INFO">通知</el-radio>
            <el-radio label="WARN">提醒</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!form.id" label="推送">
          <el-switch v-model="form.notifyAll" active-text="同时推送站内通知给所有学生" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ form.id ? '保存' : '发布' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { announcementApi } from '../../api'

const list = ref([])
const dialog = ref(false)
const saving = ref(false)
const form = reactive({ id: null, title: '', content: '', level: 'INFO', notifyAll: true })

async function load() { list.value = await announcementApi.adminList() }
load()

function openCreate() {
  Object.assign(form, { id: null, title: '', content: '', level: 'INFO', notifyAll: true })
  dialog.value = true
}
function openEdit(row) {
  Object.assign(form, { id: row.id, title: row.title, content: row.content, level: row.level, notifyAll: false })
  dialog.value = true
}
async function save() {
  if (!form.title.trim() || !form.content.trim()) { ElMessage.warning('标题和内容不能为空'); return }
  saving.value = true
  try {
    if (form.id) await announcementApi.update(form.id, { title: form.title, content: form.content, level: form.level })
    else await announcementApi.create({ title: form.title, content: form.content, level: form.level, notifyAll: form.notifyAll })
    ElMessage.success(form.id ? '已保存' : (form.notifyAll ? '已发布并推送' : '已发布'))
    dialog.value = false
    await load()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  } finally { saving.value = false }
}
async function toggle(row) {
  await announcementApi.update(row.id, { active: row.active ? 0 : 1 })
  await load()
}
async function remove(row) {
  try {
    await ElMessageBox.confirm('确认删除该公告？', '提示', { type: 'warning' })
    await announcementApi.remove(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}
</script>
