<template>
  <main class="login-page">
    <section class="login-brand">
      <div class="brand-badge">暖</div>
      <div>
        <h1>暖集商城</h1>
        <p>挑选真实商品，也可以直接询问 AI 助手。</p>
      </div>
    </section>

    <section class="login-panel surface">
      <div class="mode-switch">
        <button
          type="button"
          :class="{ active: mode === 'login' }"
          @click="mode = 'login'"
        >
          登录
        </button>
        <button
          type="button"
          :class="{ active: mode === 'register' }"
          :disabled="form.role === 'ADMIN'"
          @click="switchRegister"
        >
          注册
        </button>
      </div>

      <div class="role-picker">
        <button
          v-for="item in roles"
          :key="item.value"
          type="button"
          class="role-option"
          :class="{ active: form.role === item.value }"
          @click="selectRole(item.value)"
        >
          <component :is="item.icon" :size="19" />
          <span>{{ item.label }}</span>
        </button>
      </div>

      <el-form label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input
            v-model.trim="form.username"
            size="large"
            :prefix-icon="UserRound"
            placeholder="请输入用户名"
            @keyup.enter="submit"
          />
        </el-form-item>

        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            show-password
            :prefix-icon="LockKeyhole"
            placeholder="请输入密码"
            @keyup.enter="submit"
          />
        </el-form-item>

        <template v-if="mode === 'register'">
          <el-form-item label="确认密码">
            <el-input
              v-model="form.confirmPassword"
              size="large"
              type="password"
              show-password
              :prefix-icon="LockKeyhole"
              placeholder="再次输入密码"
            />
          </el-form-item>

          <el-form-item v-if="form.role === 'BUYER'" label="昵称">
            <el-input v-model.trim="form.nickname" size="large" placeholder="请输入昵称" />
          </el-form-item>

          <el-form-item v-if="form.role === 'SELLER'" label="店铺名称">
            <el-input v-model.trim="form.shopName" size="large" placeholder="请输入店铺名称" />
          </el-form-item>

          <el-form-item label="手机号">
            <el-input v-model.trim="form.phone" size="large" placeholder="选填" />
          </el-form-item>

          <el-form-item label="邮箱">
            <el-input v-model.trim="form.email" size="large" placeholder="选填" />
          </el-form-item>

          <el-form-item label="收货地址">
            <el-input
              v-model.trim="form.address"
              size="large"
              type="textarea"
              :rows="2"
              placeholder="选填"
            />
          </el-form-item>
        </template>

        <el-button
          class="submit-button"
          type="primary"
          size="large"
          :loading="loading"
          @click="submit"
        >
          {{ mode === 'login' ? '进入商城' : '注册并登录' }}
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  LockKeyhole,
  ShieldCheck,
  Store,
  UserRound
} from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const mode = ref('login')
const loading = ref(false)
const roles = [
  { label: '买家', value: 'BUYER', icon: UserRound },
  { label: '卖家', value: 'SELLER', icon: Store },
  { label: '管理员', value: 'ADMIN', icon: ShieldCheck }
]

const form = reactive({
  role: 'BUYER',
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  shopName: '',
  phone: '',
  email: '',
  address: ''
})

function selectRole(role) {
  form.role = role
  if (role === 'ADMIN') {
    mode.value = 'login'
  }
}

function switchRegister() {
  if (form.role !== 'ADMIN') {
    mode.value = 'register'
  }
}

function validate() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return false
  }
  if (mode.value === 'register') {
    if (form.password.length < 6) {
      ElMessage.warning('密码至少需要 6 位')
      return false
    }
    if (form.password !== form.confirmPassword) {
      ElMessage.warning('两次输入的密码不一致')
      return false
    }
    if (form.role === 'BUYER' && !form.nickname) {
      ElMessage.warning('请输入昵称')
      return false
    }
    if (form.role === 'SELLER' && !form.shopName) {
      ElMessage.warning('请输入店铺名称')
      return false
    }
  }
  return true
}

async function submit() {
  if (!validate() || loading.value) return
  loading.value = true
  try {
    const payload = {
      role: form.role,
      username: form.username,
      password: form.password
    }
    if (mode.value === 'register') {
      Object.assign(payload, {
        nickname: form.nickname || undefined,
        shopName: form.shopName || undefined,
        phone: form.phone || undefined,
        email: form.email || undefined,
        address: form.address || undefined
      })
      await auth.register(payload)
      ElMessage.success('注册成功')
    } else {
      await auth.login(payload)
      ElMessage.success('登录成功')
    }
    const redirect = route.query.redirect
    if (redirect && auth.role === 'BUYER') {
      router.replace(String(redirect))
    } else {
      router.replace(auth.defaultRoute)
    }
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  gap: 28px;
  padding: 32px 20px;
  background: #fff1e5;
}

.login-brand {
  display: flex;
  width: min(520px, 100%);
  align-items: center;
  gap: 16px;
}

.brand-badge {
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  background: var(--primary);
  font-size: 24px;
  font-weight: 900;
  box-shadow: 0 12px 24px rgba(200, 82, 36, 0.2);
}

.login-brand h1 {
  margin: 0 0 4px;
  font-size: 28px;
}

.login-brand p {
  margin: 0;
  color: var(--muted);
}

.login-panel {
  width: min(520px, 100%);
  padding: 22px;
}

.mode-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  margin-bottom: 18px;
  padding: 4px;
  border-radius: 8px;
  background: var(--surface-strong);
}

.mode-switch button {
  min-height: 38px;
  border: 0;
  border-radius: 6px;
  color: var(--muted);
  background: transparent;
  cursor: pointer;
}

.mode-switch button.active {
  color: var(--primary-dark);
  background: var(--surface);
  font-weight: 700;
}

.mode-switch button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.role-picker {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 20px;
}

.role-option {
  display: flex;
  min-height: 48px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid var(--line);
  border-radius: 8px;
  color: var(--muted);
  background: var(--surface);
  cursor: pointer;
}

.role-option.active {
  border-color: var(--primary);
  color: var(--primary-dark);
  background: var(--primary-soft);
  font-weight: 700;
}

.submit-button {
  width: 100%;
  margin-top: 4px;
}

@media (max-width: 520px) {
  .login-panel {
    padding: 18px;
  }
}
</style>
