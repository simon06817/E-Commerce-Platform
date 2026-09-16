<template>
  <nav class="bottom-nav">
    <button
      v-for="item in items"
      :key="item.path"
      class="nav-item"
      :class="{ active: route.path === item.path }"
      type="button"
      @click="navigate(item)"
    >
      <el-badge :value="item.badge || ''" :hidden="!item.badge">
        <component :is="item.icon" :size="21" />
      </el-badge>
      <span>{{ item.label }}</span>
    </button>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bot, House, ShoppingCart, UserRound } from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()

const items = computed(() => [
  { label: '首页', path: '/home', icon: House, protected: false },
  { label: 'AI助手', path: '/assistant', icon: Bot, protected: true },
  {
    label: '购物车',
    path: '/cart',
    icon: ShoppingCart,
    protected: true,
    badge: cart.count
  },
  { label: '我的', path: '/profile', icon: UserRound, protected: true }
])

function navigate(item) {
  if (item.protected && !auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: item.path } })
    return
  }
  if (!item.protected && auth.role === 'BUYER') {
    router.push(item.path)
    return
  }
  if (item.protected && auth.role !== 'BUYER') {
    router.push(auth.defaultRoute)
    return
  }
  router.push(item.path)
}
</script>

<style scoped>
.bottom-nav {
  position: fixed;
  z-index: 30;
  right: 0;
  bottom: 0;
  left: 0;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  width: min(720px, 100%);
  min-height: 70px;
  margin: 0 auto;
  padding: 8px 12px calc(8px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--line);
  background: rgba(255, 253, 250, 0.96);
  box-shadow: 0 -10px 28px rgba(127, 74, 42, 0.08);
  backdrop-filter: blur(14px);
}

.nav-item {
  display: grid;
  place-items: center;
  gap: 2px;
  border: 0;
  color: #8a766c;
  background: transparent;
  font-size: 12px;
  cursor: pointer;
}

.nav-item.active {
  color: var(--primary-dark);
  font-weight: 700;
}
</style>
