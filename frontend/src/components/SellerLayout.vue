<template>
  <div class="seller-layout">
    <aside class="seller-sidebar">
      <div class="seller-brand">
        <span class="seller-mark"><Store :size="22" /></span>
        <div>
          <strong>卖家中心</strong>
          <span>{{ auth.displayName || auth.username }}</span>
        </div>
      </div>

      <nav class="seller-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="seller-nav-item"
        >
          <component :is="item.icon" :size="19" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <el-button class="seller-logout" :icon="LogOut" @click="logout">
        退出登录
      </el-button>
    </aside>

    <div class="seller-main">
      <header class="seller-topbar">
        <button class="mobile-menu" type="button" @click="mobileNavVisible = true">
          <Menu :size="22" />
        </button>
        <div>
          <strong>{{ currentTitle }}</strong>
          <span>管理店铺商品、订单与售后</span>
        </div>
        <div class="seller-topbar-actions">
          <el-badge :value="unreadCount" :hidden="!unreadCount">
            <el-button circle :icon="Bell" @click="openNotifications" />
          </el-badge>
          <el-button type="primary" :icon="Bot" @click="router.push('/seller/assistant')">
            AI 助手
          </el-button>
        </div>
      </header>

      <main class="seller-content">
        <router-view />
      </main>
    </div>

    <el-drawer
      v-model="mobileNavVisible"
      title="卖家导航"
      direction="ltr"
      size="min(280px, 82vw)"
    >
      <nav class="mobile-seller-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="seller-nav-item"
          @click="mobileNavVisible = false"
        >
          <component :is="item.icon" :size="19" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
    </el-drawer>

    <el-drawer
      v-model="notificationsVisible"
      title="订单通知"
      size="min(480px, 92vw)"
    >
      <div v-if="loadingNotifications" class="notification-list">
        <el-skeleton v-for="item in 3" :key="item" animated :rows="2" />
      </div>
      <EmptyState
        v-else-if="!notifications.length"
        title="暂无通知"
        description="订单状态变化后会在这里提醒"
      />
      <div v-else class="notification-list">
        <button
          v-for="item in notifications"
          :key="item.id"
          class="notification-item"
          :class="{ unread: !item.readFlag }"
          type="button"
          @click="markNotificationRead(item)"
        >
          <span class="notification-dot" />
          <span class="notification-content">
            <strong>{{ localizeMessage(item.title) }}</strong>
            <span>{{ localizeMessage(item.content) }}</span>
            <time>{{ formatDate(item.createTime) }}</time>
          </span>
        </button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Bell,
  Bot,
  ClipboardList,
  LayoutDashboard,
  LogOut,
  Menu,
  Package,
  RotateCcw,
  Star,
  Store,
  UserRound
} from 'lucide-vue-next'
import EmptyState from './EmptyState.vue'
import { notificationApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { formatDate } from '../utils/format'
import { localizeMessage } from '../utils/message'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const mobileNavVisible = ref(false)
const notificationsVisible = ref(false)
const loadingNotifications = ref(false)
const notifications = ref([])

const navItems = [
  { label: '经营概览', path: '/seller/dashboard', icon: LayoutDashboard },
  { label: '商品管理', path: '/seller/products', icon: Package },
  { label: '订单发货', path: '/seller/orders', icon: ClipboardList },
  { label: '退款审核', path: '/seller/returns', icon: RotateCcw },
  { label: '评价管理', path: '/seller/reviews', icon: Star },
  { label: '店铺设置', path: '/seller/profile', icon: UserRound },
  { label: 'AI 助手', path: '/seller/assistant', icon: Bot }
]

const currentTitle = computed(
  () => navItems.find((item) => item.path === route.path)?.label || '卖家中心'
)
const unreadCount = computed(
  () => notifications.value.filter((item) => !item.readFlag).length
)

async function loadNotifications() {
  loadingNotifications.value = true
  try {
    const data = await notificationApi.page({ page: 1, size: 50 })
    notifications.value = data.records || []
  } catch {
    notifications.value = []
  } finally {
    loadingNotifications.value = false
  }
}

function openNotifications() {
  notificationsVisible.value = true
  loadNotifications()
}

async function markNotificationRead(item) {
  if (item.readFlag) return
  try {
    await notificationApi.markRead(item.id)
    item.readFlag = true
  } catch (error) {
    ElMessage.error(error.message || '通知更新失败')
  }
}

async function logout() {
  await auth.logout()
  router.replace('/login')
}

onMounted(loadNotifications)
</script>

<style scoped>
.seller-layout {
  display: grid;
  min-height: 100vh;
  grid-template-columns: 232px minmax(0, 1fr);
  background: #f8f3ed;
}

.seller-sidebar {
  position: sticky;
  top: 0;
  display: flex;
  height: 100vh;
  flex-direction: column;
  gap: 20px;
  padding: 22px 16px;
  color: #fff;
  background: #4d3528;
}

.seller-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.seller-brand div {
  display: grid;
  min-width: 0;
}

.seller-brand strong {
  font-size: 19px;
}

.seller-brand span:last-child {
  overflow: hidden;
  color: #e7d4c7;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.seller-mark {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  background: var(--primary);
}

.seller-nav,
.mobile-seller-nav {
  display: grid;
  gap: 6px;
}

.seller-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 0 12px;
  border-radius: 7px;
  color: #e9d9cd;
  text-decoration: none;
  transition: background 0.16s, color 0.16s;
}

.seller-nav-item:hover,
.seller-nav-item.router-link-active {
  color: #fff;
  background: rgba(232, 111, 58, 0.88);
}

.seller-logout {
  margin-top: auto;
}

.seller-main {
  min-width: 0;
}

.seller-topbar {
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px clamp(18px, 3vw, 36px);
  border-bottom: 1px solid var(--line);
  background: rgba(255, 253, 250, 0.96);
}

.seller-topbar > div:first-of-type {
  display: grid;
}

.seller-topbar strong {
  font-size: 20px;
}

.seller-topbar span {
  color: var(--muted);
  font-size: 12px;
}

.seller-topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mobile-menu {
  display: none;
  border: 0;
  color: var(--text);
  background: transparent;
}

.seller-content {
  padding: 22px clamp(16px, 3vw, 36px) 42px;
}

.notification-list {
  display: grid;
  gap: 10px;
}

.notification-item {
  display: grid;
  grid-template-columns: 10px 1fr;
  align-items: start;
  gap: 10px;
  width: 100%;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  color: var(--text);
  background: var(--surface);
  text-align: left;
  cursor: pointer;
}

.notification-item.unread {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.notification-dot {
  width: 8px;
  height: 8px;
  margin-top: 5px;
  border-radius: 50%;
  background: #d8c4b8;
}

.notification-item.unread .notification-dot {
  background: var(--primary);
}

.notification-content {
  display: grid;
  gap: 6px;
}

.notification-content span,
.notification-content time {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.55;
}

.mobile-seller-nav {
  padding: 0;
}

.mobile-seller-nav .seller-nav-item {
  color: var(--text);
}

.mobile-seller-nav .seller-nav-item.router-link-active {
  color: #fff;
}

@media (max-width: 900px) {
  .seller-layout {
    display: block;
  }

  .seller-sidebar {
    display: none;
  }

  .mobile-menu {
    display: grid;
    place-items: center;
  }
}

@media (max-width: 560px) {
  .seller-topbar {
    align-items: flex-start;
  }

  .seller-topbar-actions .el-button span {
    display: none;
  }
}
</style>
