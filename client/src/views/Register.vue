<template>
  <div class="reg-wrap">
    <el-card class="reg-card">
      <h2 style="margin:0 0 4px">注册新账号</h2>
      <p style="color:#8a93a6;margin:0 0 20px">注册后默认为学生角色，可立即登录预约</p>
      <el-form :model="form" @submit.prevent="submit">
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="用户名（3-32位）" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.realName" size="large" placeholder="姓名" :prefix-icon="Avatar" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码（至少6位）" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.confirm" size="large" type="password" show-password placeholder="确认密码" :prefix-icon="Lock" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="submit">注 册</el-button>
      </el-form>
      <div style="text-align:center;margin-top:14px">
        已有账号？<el-link type="primary" @click="$router.push('/login')">去登录</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Avatar } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { authApi } from '../api'

const router = useRouter()
const user = useUserStore()
const form = reactive({ username: '', realName: '', password: '', confirm: '' })
const loading = ref(false)

async function submit() {
  if (!form.username || !form.realName || !form.password) { ElMessage.warning('请完整填写'); return }
  if (form.password !== form.confirm) { ElMessage.warning('两次密码不一致'); return }
  loading.value = true
  try {
    const data = await authApi.register({ username: form.username, password: form.password, realName: form.realName })
    // register 直接返回登录态
    user.token = data.token; user.role = data.role; user.userInfo = data.userInfo
    localStorage.setItem('satoken', data.token)
    localStorage.setItem('role', data.role)
    localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
    ElMessage.success('注册成功，已自动登录')
    router.push('/student')
  } catch (e) { /* 拦截器已提示 */ } finally { loading.value = false }
}
</script>

<style scoped>
.reg-wrap { height:100vh; display:grid; place-items:center; background:linear-gradient(135deg,#3b6cff,#8f5bff); }
.reg-card { width:380px; }
</style>
