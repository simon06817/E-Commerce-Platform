import { createRouter, createWebHistory } from 'vue-router'
import { getSession, roleHome } from '../api/session'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/home',
    name: 'home',
    component: () => import('../views/HomeView.vue')
  },
  {
    path: '/product/:id',
    name: 'product-detail',
    component: () => import('../views/ProductDetailView.vue')
  },
  {
    path: '/assistant',
    name: 'assistant',
    component: () => import('../views/AssistantView.vue'),
    meta: { requiresAuth: true, role: 'BUYER' }
  },
  {
    path: '/cart',
    name: 'cart',
    component: () => import('../views/CartView.vue'),
    meta: { requiresAuth: true, role: 'BUYER' }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/ProfileView.vue'),
    meta: { requiresAuth: true, role: 'BUYER' }
  },
  {
    path: '/seller',
    name: 'seller-placeholder',
    component: () => import('../views/SellerPlaceholderView.vue'),
    meta: { requiresAuth: true, role: 'SELLER' }
  },
  {
    path: '/admin',
    name: 'admin-placeholder',
    component: () => import('../views/AdminPlaceholderView.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const session = getSession()
  if (to.name === 'login' && session) {
    return roleHome(session.role)
  }
  if (!to.meta.requiresAuth) {
    return true
  }
  if (!session) {
    return {
      name: 'login',
      query: { redirect: to.fullPath }
    }
  }
  if (to.meta.role && session.role !== to.meta.role) {
    return roleHome(session.role)
  }
  return true
})

export default router
