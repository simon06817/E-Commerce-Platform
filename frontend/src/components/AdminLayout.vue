<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="admin-brand">
        <span class="admin-mark"><ShieldCheck :size="22" /></span>
        <div>
          <strong>管理后台</strong>
          <span>{{ auth.displayName || auth.username }}</span>
        </div>
      </div>

      <nav class="admin-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="admin-nav-item"
        >
          <component :is="item.icon" :size="19" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <el-button class="admin-logout" :icon="LogOut" @click="logout">
        退出登录
      </el-button>
    </aside>

    <div class="admin-main">
      <header class="admin-topbar">
        <button class="mobile-menu" type="button" @click="mobileNavVisible = true">
          <Menu :size="22" />
        </button>
        <div>
          <strong>{{ currentTitle }}</strong>
          <span>平台用户、订单和基础数据管理</span>
        </div>
        <div class="admin-identity">
          <span>管理员</span>
          <strong>{{ auth.username }}</strong>
        </div>
      </header>

      <main class="admin-content">
        <router-view />
      </main>
    </div>

    <el-drawer
      v-model="mobileNavVisible"
      title="管理导航"
      direction="ltr"
      size="min(280px, 82vw)"
    >
      <nav class="mobile-admin-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="admin-nav-item"
          @click="mobileNavVisible = false"
        >
          <component :is="item.icon" :size="19" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import {
  Bot,
  ClipboardList,
  LayoutDashboard,
  LogOut,
  Menu,
  ShieldCheck,
  Store,
  Tags,
  UserCog,
  Users
} from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const mobileNavVisible = ref(false)

const navItems = [
  { label: '数据概览', path: '/admin/dashboard', icon: LayoutDashboard },
  { label: '订单管理', path: '/admin/orders', icon: ClipboardList },
  { label: '买家管理', path: '/admin/buyers', icon: Users },
  { label: '卖家管理', path: '/admin/sellers', icon: Store },
  { label: '分类管理', path: '/admin/categories', icon: Tags },
  { label: 'AI 助手', path: '/admin/assistant', icon: Bot },
  { label: '账号设置', path: '/admin/profile', icon: UserCog }
]

const currentTitle = computed(
  () => navItems.find((item) => item.path === route.path)?.label || '管理后台'
)

async function logout() {
  await auth.logout()
  router.replace('/login')
}
</script>

<style scoped>
.admin-layout {
  display: grid;
  min-height: 100vh;
  grid-template-columns: 232px minmax(0, 1fr);
  background: #f5f6f8;
}

.admin-sidebar {
  position: sticky;
  top: 0;
  display: flex;
  height: 100vh;
  flex-direction: column;
  gap: 20px;
  padding: 22px 16px;
  color: #fff;
  background: #243447;
}

.admin-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.admin-brand div {
  display: grid;
  min-width: 0;
}

.admin-brand strong {
  font-size: 19px;
}

.admin-brand span:last-child {
  overflow: hidden;
  color: #c7d1dd;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-mark {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  background: #c8613d;
}

.admin-nav,
.mobile-admin-nav {
  display: grid;
  gap: 6px;
}

.admin-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 0 12px;
  border-radius: 7px;
  color: #d6dee8;
  text-decoration: none;
  transition: background 0.16s, color 0.16s;
}

.admin-nav-item:hover,
.admin-nav-item.router-link-active {
  color: #fff;
  background: #c8613d;
}

.admin-logout {
  margin-top: auto;
}

.admin-main {
  min-width: 0;
}

.admin-topbar {
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px clamp(18px, 3vw, 36px);
  border-bottom: 1px solid #dfe3e8;
  background: rgba(255, 255, 255, 0.96);
}

.admin-topbar > div:first-of-type {
  display: grid;
}

.admin-topbar strong {
  font-size: 20px;
}

.admin-topbar span {
  color: #718096;
  font-size: 12px;
}

.admin-identity {
  display: grid;
  justify-items: end;
}

.mobile-menu {
  display: none;
  border: 0;
  color: #243447;
  background: transparent;
}

.admin-content {
  padding: 22px clamp(16px, 3vw, 36px) 42px;
}

.mobile-admin-nav {
  padding: 0;
}

.mobile-admin-nav .admin-nav-item {
  color: #243447;
}

.mobile-admin-nav .admin-nav-item.router-link-active {
  color: #fff;
}

@media (max-width: 900px) {
  .admin-layout {
    display: block;
  }

  .admin-sidebar {
    display: none;
  }

  .mobile-menu {
    display: grid;
    place-items: center;
  }
}

@media (max-width: 560px) {
  .admin-identity {
    display: none;
  }
}
</style>
