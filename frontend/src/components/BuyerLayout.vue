<template>
  <div class="page buyer-layout">
    <header class="buyer-header">
      <div class="page-inner header-inner">
        <button class="brand" type="button" @click="router.push('/home')">
          <span class="brand-mark">暖</span>
          <span>暖集商城</span>
        </button>
        <div class="header-actions">
          <template v-if="auth.role === 'BUYER'">
            <el-button text :icon="Heart" @click="router.push('/profile')">
              {{ auth.displayName }}
            </el-button>
          </template>
          <el-button
            v-else-if="auth.isLoggedIn"
            text
            @click="router.push(auth.defaultRoute)"
          >
            {{ auth.role === 'SELLER' ? '卖家中心' : '管理后台' }}
          </el-button>
          <el-button v-else type="primary" :icon="LogIn" @click="goLogin">
            登录 / 注册
          </el-button>
        </div>
      </div>
    </header>

    <main class="page-inner buyer-main">
      <slot />
    </main>

    <BottomNav v-if="showNav" />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Heart, LogIn } from 'lucide-vue-next'
import BottomNav from './BottomNav.vue'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'

defineProps({
  showNav: { type: Boolean, default: true }
})

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const cart = useCartStore()

const loginRedirect = computed(() => route.fullPath)

function goLogin() {
  router.push({ name: 'login', query: { redirect: loginRedirect.value } })
}

onMounted(() => {
  if (auth.role === 'BUYER') {
    cart.ensureLoaded().catch(() => {})
  }
})
</script>

<style scoped>
.buyer-layout {
  background: var(--bg);
}

.buyer-header {
  position: sticky;
  z-index: 20;
  top: 0;
  border-bottom: 1px solid rgba(239, 214, 197, 0.9);
  background: rgba(255, 248, 241, 0.95);
  backdrop-filter: blur(14px);
}

.header-inner {
  display: flex;
  min-height: 64px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0;
  border: 0;
  color: var(--text);
  background: transparent;
  font-size: 18px;
  font-weight: 800;
  cursor: pointer;
}

.brand-mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  background: var(--primary);
}

.header-actions {
  display: flex;
  align-items: center;
}

.buyer-main {
  padding-top: 22px;
}

@media (max-width: 640px) {
  .brand {
    font-size: 16px;
  }

  .brand-mark {
    width: 31px;
    height: 31px;
  }
}
</style>
