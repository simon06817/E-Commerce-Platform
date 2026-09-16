<template>
  <main class="placeholder-page">
    <section class="placeholder-panel surface">
      <ShieldCheck :size="42" />
      <h1>管理后台</h1>
      <p>管理员页面将在买家端完成后继续开发。</p>
      <el-button type="primary" :icon="LogOut" @click="logout">退出登录</el-button>
    </section>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { LogOut, ShieldCheck } from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

async function logout() {
  await auth.logout()
  router.replace('/login')
}
</script>

<style scoped>
.placeholder-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 20px;
  background: #fff1e5;
}

.placeholder-panel {
  display: grid;
  width: min(460px, 100%);
  place-items: center;
  gap: 12px;
  padding: 42px;
  text-align: center;
}

.placeholder-panel h1 {
  margin: 0;
}

.placeholder-panel p {
  margin: 0 0 10px;
  color: var(--muted);
}
</style>
