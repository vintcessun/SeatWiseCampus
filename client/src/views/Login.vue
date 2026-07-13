<template>
  <div class="login-wrap">
    <div class="login-lang"><LanguageSwitcher /></div>
    <div class="login-hero">
      <div class="aurora a1"></div>
      <div class="aurora a2"></div>
      <div class="aurora a3"></div>
      <div class="hero-content">
        <div class="brand" style="font-size:26px"><span class="logo" style="width:44px;height:44px;font-size:24px">🎓</span> SeatWise Campus</div>
        <h1>{{ $t('login.heroTitle1') }}<br/>{{ $t('login.heroTitle2') }}</h1>
        <p>{{ $t('login.heroSubtitle') }}</p>
        <ul>
          <li>{{ $t('login.feature1') }}</li>
          <li>{{ $t('login.feature2') }}</li>
          <li>{{ $t('login.feature3') }}</li>
        </ul>
      </div>
    </div>
    <div class="login-panel">
      <el-card class="login-card">
        <el-tabs v-model="activeTab" class="login-tabs">
          <el-tab-pane :label="$t('login.tabLogin')" name="login">
            <el-form :model="form" @submit.prevent="submitLogin">
              <el-form-item>
                <el-input v-model="form.username" size="large" :placeholder="$t('login.username')" :prefix-icon="User" :aria-label="$t('login.username')" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="form.password" size="large" type="password" show-password :placeholder="$t('login.password')" :prefix-icon="Lock" :aria-label="$t('login.password')" @keyup.enter="submitLogin" />
              </el-form-item>
              <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="submitLogin">{{ $t('login.login') }}</el-button>
            </el-form>
            <el-divider>{{ $t('login.quickLogin') }}</el-divider>
            <div style="display:flex;gap:10px;flex-wrap:wrap">
              <el-button @click="quick('admin','admin123')">{{ $t('login.quickAdmin') }}</el-button>
              <el-button @click="quick('student1','123456')">{{ $t('login.quickStudent1') }}</el-button>
              <el-button @click="quick('student2','123456')">{{ $t('login.quickStudent2') }}</el-button>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="$t('login.tabRegister')" name="register">
            <p style="color:#8a93a6;margin:0 0 16px">{{ $t('login.registerHint') }}</p>
            <el-form :model="regForm" @submit.prevent="submitRegister">
              <el-form-item>
                <el-input v-model="regForm.username" size="large" :placeholder="$t('login.usernameRule')" :prefix-icon="User" :aria-label="$t('login.username')" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="regForm.realName" size="large" :placeholder="$t('login.realName')" :prefix-icon="Avatar" :aria-label="$t('login.realName')" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="regForm.password" size="large" type="password" show-password :placeholder="$t('login.passwordRule')" :prefix-icon="Lock" :aria-label="$t('login.password')" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="regForm.confirm" size="large" type="password" show-password :placeholder="$t('login.confirmPassword')" :prefix-icon="Lock" :aria-label="$t('login.confirmPassword')" />
              </el-form-item>
              <el-form-item>
                <div style="display:flex;gap:10px;width:100%;align-items:center">
                  <el-input v-model="regForm.captchaCode" size="large" :placeholder="$t('login.captcha')" :prefix-icon="Key" style="flex:1" :aria-label="$t('login.captcha')" @keyup.enter="submitRegister" />
                  <img v-if="captcha.image" :src="captcha.image" :alt="$t('login.captcha')" :title="$t('login.captchaRefresh')"
                    style="height:42px;width:120px;border-radius:6px;cursor:pointer;border:1px solid var(--el-border-color)" @click="loadCaptcha" />
                </div>
              </el-form-item>
              <el-button type="primary" size="large" style="width:100%" :loading="regLoading" @click="submitRegister">{{ $t('login.register') }}</el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane :label="$t('login.tabForgot')" name="forgot">
            <p style="color:#8a93a6;margin:0 0 16px">{{ $t('login.forgotHint') }}</p>
            <el-form :model="resetForm" @submit.prevent="submitReset">
              <el-form-item>
                <el-input v-model="resetForm.username" size="large" :placeholder="$t('login.username')" :prefix-icon="User" :aria-label="$t('login.username')" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="resetForm.realName" size="large" :placeholder="$t('login.realNameVerify')" :prefix-icon="Avatar" :aria-label="$t('login.realName')" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="resetForm.newPassword" size="large" type="password" show-password :placeholder="$t('login.newPassword')" :prefix-icon="Lock" :aria-label="$t('login.newPassword')" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="resetForm.confirm" size="large" type="password" show-password :placeholder="$t('login.confirmNewPassword')" :prefix-icon="Lock" :aria-label="$t('login.confirmNewPassword')" />
              </el-form-item>
              <el-form-item>
                <div style="display:flex;gap:10px;width:100%;align-items:center">
                  <el-input v-model="resetForm.captchaCode" size="large" :placeholder="$t('login.captcha')" :prefix-icon="Key" style="flex:1" :aria-label="$t('login.captcha')" @keyup.enter="submitReset" />
                  <img v-if="captcha.image" :src="captcha.image" :alt="$t('login.captcha')" :title="$t('login.captchaRefresh')"
                    style="height:42px;width:120px;border-radius:6px;cursor:pointer;border:1px solid var(--el-border-color)" @click="loadCaptcha" />
                </div>
              </el-form-item>
              <el-button type="primary" size="large" style="width:100%" :loading="resetLoading" @click="submitReset">{{ $t('login.resetPassword') }}</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Avatar, Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { authApi } from '../api'
import { t } from '../i18n'
import LanguageSwitcher from '../components/LanguageSwitcher.vue'

const router = useRouter()
const user = useUserStore()
const activeTab = ref('login')

// --- 登录 ---
const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function submitLogin() {
  if (!form.username || !form.password) { ElMessage.warning(t('login.msgNeedUserPass')); return }
  loading.value = true
  try {
    const data = await user.login({ ...form })
    ElMessage.success(t('login.msgLoginSuccess'))
    router.push((data.role === 'ADMIN' || data.role === 'ADMIN_SUB') ? '/admin' : '/student')
  } catch (e) { /* 错误已由拦截器提示 */ } finally { loading.value = false }
}

function quick(u, p) {
  form.username = u
  form.password = p
  submitLogin()
}

// --- 注册 ---
const regForm = reactive({ username: '', realName: '', password: '', confirm: '', captchaCode: '' })
const regLoading = ref(false)

async function submitRegister() {
  if (!regForm.username || !regForm.realName || !regForm.password) { ElMessage.warning(t('login.msgFillAll')); return }
  if (regForm.password !== regForm.confirm) { ElMessage.warning(t('login.msgPwdMismatch')); return }
  if (!regForm.captchaCode) { ElMessage.warning(t('login.msgNeedCaptcha')); return }
  regLoading.value = true
  try {
    const data = await authApi.register({
      username: regForm.username, password: regForm.password,
      realName: regForm.realName,
      captchaId: captcha.captchaId, captchaCode: regForm.captchaCode
    })
    user.token = data.token; user.role = data.role; user.userInfo = data.userInfo
    localStorage.setItem('satoken', data.token)
    localStorage.setItem('role', data.role)
    localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
    ElMessage.success(t('login.msgRegisterSuccess'))
    router.push('/student')
  } catch (e) { loadCaptcha() } finally { regLoading.value = false }
}

// --- 忘记密码 ---
const resetForm = reactive({ username: '', realName: '', newPassword: '', confirm: '', captchaCode: '' })
const resetLoading = ref(false)

async function submitReset() {
  if (!resetForm.username || !resetForm.realName || !resetForm.newPassword) { ElMessage.warning(t('login.msgFillAll')); return }
  if (resetForm.newPassword !== resetForm.confirm) { ElMessage.warning(t('login.msgPwdMismatch')); return }
  if (!resetForm.captchaCode) { ElMessage.warning(t('login.msgNeedCaptcha')); return }
  resetLoading.value = true
  try {
    await authApi.resetPassword({
      username: resetForm.username, realName: resetForm.realName,
      newPassword: resetForm.newPassword,
      captchaId: captcha.captchaId, captchaCode: resetForm.captchaCode
    })
    ElMessage.success(t('login.msgResetSuccess'))
    resetForm.username = ''; resetForm.realName = ''; resetForm.newPassword = ''; resetForm.confirm = ''; resetForm.captchaCode = ''
    activeTab.value = 'login'
  } catch (e) { loadCaptcha() } finally { resetLoading.value = false }
}

// --- 验证码 ---
const captcha = reactive({ captchaId: '', image: '' })

async function loadCaptcha() {
  try {
    const d = await authApi.captcha()
    captcha.captchaId = d.captchaId
    captcha.image = d.image
  } catch (e) { /* ignore */ }
}

watch(activeTab, (tab) => {
  if (tab === 'register') {
    regForm.captchaCode = ''
    loadCaptcha()
  } else if (tab === 'forgot') {
    resetForm.captchaCode = ''
    loadCaptcha()
  }
})
</script>

<style scoped>
.login-wrap { display:flex; height:100vh; position:relative; }
.login-lang { position:absolute; top:18px; right:20px; z-index:10; }
.login-hero {
  flex:1; position:relative; overflow:hidden; color:#fff;
  background:linear-gradient(135deg,#2f52d8,#6f45e6 55%,#8f5bff);
  padding:60px; display:flex; flex-direction:column; justify-content:center; gap:16px;
}
.hero-content { position:relative; z-index:2; }
.login-hero h1 { font-size:40px; line-height:1.2; margin:10px 0; text-shadow:0 2px 20px rgba(0,0,0,.15); }
.login-hero p { font-size:15px; opacity:.92; }
.login-hero ul { list-style:none; padding:0; margin:20px 0 0; line-height:2; opacity:.95; }

.aurora { position:absolute; border-radius:50%; filter:blur(60px); opacity:.55; z-index:1; }
.a1 { width:420px; height:420px; background:#5b8cff; top:-80px; left:-60px; animation:float1 14s ease-in-out infinite; }
.a2 { width:360px; height:360px; background:#c46bff; bottom:-90px; left:20%; animation:float2 18s ease-in-out infinite; }
.a3 { width:300px; height:300px; background:#22c1c3; top:30%; right:-70px; animation:float3 16s ease-in-out infinite; }
@keyframes float1 { 0%,100%{ transform:translate(0,0) scale(1); } 50%{ transform:translate(40px,30px) scale(1.12); } }
@keyframes float2 { 0%,100%{ transform:translate(0,0) scale(1); } 50%{ transform:translate(-30px,-40px) scale(1.08); } }
@keyframes float3 { 0%,100%{ transform:translate(0,0) scale(1); } 50%{ transform:translate(-25px,25px) scale(1.15); } }

.login-panel {
  width:460px; display:grid; place-items:center;
  background:
    radial-gradient(600px 300px at 50% -10%, rgba(91,140,255,.10), transparent 60%),
    #fff;
}
.login-card { width:380px; border:none; box-shadow:0 12px 40px rgba(31,45,80,.08); border-radius:16px; }
:deep(.login-card .el-input__wrapper) { border-radius:10px; }
:deep(.login-tabs .el-tabs__header) { margin-bottom:20px; }
:deep(.login-tabs .el-tabs__item) { font-size:15px; }
@media (max-width: 820px){ .login-hero{ display:none; } .login-panel{ width:100%; } }
</style>
