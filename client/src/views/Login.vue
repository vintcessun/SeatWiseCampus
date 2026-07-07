<template>
  <div class="login-wrap">
    <div class="login-hero">
      <div class="brand" style="font-size:26px"><span class="logo" style="width:44px;height:44px;font-size:24px">🎓</span> SeatWise Campus</div>
      <h1>智能校园自习室<br/>预约管理平台</h1>
      <p>实时座位热力图 · 时间片并发选座 · 超时释放 · 爽约黑名单 · 积分排行</p>
      <ul>
        <li>✓ 初始化快照 + SSE 增量，多端座位状态秒级同步</li>
        <li>✓ Redisson 锁 + MySQL 唯一索引，杜绝并发双占</li>
        <li>✓ 到点自动释放与自动完成，公平使用</li>
      </ul>
    </div>
    <div class="login-panel">
      <el-card class="login-card">
        <h2 style="margin:0 0 4px">登录</h2>
        <p style="color:#8a93a6;margin:0 0 20px">请输入账号密码，或点击下方快捷登录</p>
        <el-form :model="form" @submit.prevent="submit">
          <el-form-item>
            <el-input v-model="form.username" size="large" placeholder="用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码" :prefix-icon="Lock" @keyup.enter="submit" />
          </el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="submit">登 录</el-button>
        </el-form>
        <el-divider>演示快捷登录</el-divider>
        <div style="display:flex;gap:10px;flex-wrap:wrap">
          <el-button @click="quick('admin','admin123')">管理员 admin</el-button>
          <el-button @click="quick('student1','123456')">学生 张三</el-button>
          <el-button @click="quick('student2','123456')">学生 李四</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const user = useUserStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function submit() {
  if (!form.username || !form.password) { ElMessage.warning('请输入用户名和密码'); return }
  loading.value = true
  try {
    const data = await user.login({ ...form })
    ElMessage.success('登录成功')
    router.push(data.role === 'ADMIN' ? '/admin' : '/student')
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    loading.value = false
  }
}

function quick(u, p) {
  form.username = u
  form.password = p
  submit()
}
</script>

<style scoped>
.login-wrap { display:flex; height:100vh; }
.login-hero {
  flex:1; background:linear-gradient(135deg,#3b6cff,#8f5bff); color:#fff;
  padding:60px; display:flex; flex-direction:column; justify-content:center; gap:16px;
}
.login-hero h1 { font-size:40px; line-height:1.2; margin:10px 0; }
.login-hero p { font-size:15px; opacity:.9; }
.login-hero ul { list-style:none; padding:0; margin:20px 0 0; line-height:2; opacity:.95; }
.login-panel { width:460px; display:grid; place-items:center; background:#fff; }
.login-card { width:360px; border:none; box-shadow:none; }
@media (max-width: 820px){ .login-hero{ display:none; } .login-panel{ width:100%; } }
</style>
