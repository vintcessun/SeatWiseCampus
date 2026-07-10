<template>
  <div class="page">
    <div style="display:flex;justify-content:space-between;align-items:center">
      <div>
        <div class="page-title">管理员管理</div>
        <div class="page-sub">主管理员可创建子管理员（ADMIN_SUB）。子管理员可管理自习室/座位/公告/报表，但不能管理其他管理员。</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="dialog = true">新增子管理员</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="list" style="width:100%">
        <el-table-column prop="username" label="用户名" width="200" />
        <el-table-column prop="realName" label="姓名" width="200" />
        <el-table-column label="角色" width="160">
          <template #default="{ row }">
            <el-tag :type="row.primary ? 'danger' : 'primary'" effect="plain">
              {{ row.primary ? '主管理员' : '子管理员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button v-if="!row.primary" size="small" type="danger" plain @click="remove(row)">删除</el-button>
            <span v-else style="color:var(--el-text-color-secondary);font-size:13px">主管理员不可删除</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" title="新增子管理员" width="420px">
      <el-form label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" placeholder="登录用户名" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" placeholder="真实姓名" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminUsersApi } from '../../api'

const list = ref([])
const dialog = ref(false)
const saving = ref(false)
const form = reactive({ username: '', realName: '', password: '' })

onMounted(load)
async function load() { list.value = await adminUsersApi.list() }

async function save() {
  if (!form.username || !form.realName || !form.password) { ElMessage.warning('请完整填写'); return }
  if (form.password.length < 6) { ElMessage.warning('密码至少 6 位'); return }
  saving.value = true
  try {
    await adminUsersApi.create({ ...form })
    ElMessage.success('子管理员已创建')
    dialog.value = false
    form.username = ''; form.realName = ''; form.password = ''
    await load()
  } catch (e) { /* 拦截器提示 */ } finally { saving.value = false }
}
async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除子管理员「${row.realName}」？`, '删除', { type: 'warning' })
    await adminUsersApi.remove(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) { if (e !== 'cancel') { /* 拦截器提示 */ } }
}
</script>
