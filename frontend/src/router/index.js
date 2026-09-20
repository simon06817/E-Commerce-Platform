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
    component: () => import('../components/SellerLayout.vue'),
    meta: { requiresAuth: true, role: 'SELLER' },
    children: [
      {
        path: '',
        redirect: '/seller/dashboard'
      },
      {
        path: 'dashboard',
        name: 'seller-dashboard',
        component: () => import('../views/SellerDashboardView.vue')
      },
      {
        path: 'products',
        name: 'seller-products',
        component: () => import('../views/SellerProductsView.vue')
      },
      {
        path: 'orders',
        name: 'seller-orders',
        component: () => import('../views/SellerOrdersView.vue')
      },
      {
        path: 'returns',
        name: 'seller-returns',
        component: () => import('../views/SellerReturnsView.vue')
      },
      {
        path: 'reviews',
        name: 'seller-reviews',
        component: () => import('../views/SellerReviewsView.vue')
      },
      {
        path: 'profile',
        name: 'seller-profile',
        component: () => import('../views/SellerProfileView.vue')
      },
      {
        path: 'assistant',
        name: 'seller-assistant',
        component: () => import('../views/SellerAssistantView.vue')
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('../components/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      {
        path: '',
        redirect: '/admin/dashboard'
      },
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: () => import('../views/AdminDashboardView.vue')
      },
      {
        path: 'orders',
        name: 'admin-orders',
        component: () => import('../views/AdminOrdersView.vue')
      },
      {
        path: 'buyers',
        name: 'admin-buyers',
        component: () => import('../views/AdminBuyersView.vue')
      },
      {
        path: 'sellers',
        name: 'admin-sellers',
        component: () => import('../views/AdminSellersView.vue')
      },
      {
        path: 'categories',
        name: 'admin-categories',
        component: () => import('../views/AdminCategoriesView.vue')
      },
      {
        path: 'assistant',
        name: 'admin-assistant',
        component: () => import('../views/AdminAssistantView.vue')
      },
      {
        path: 'profile',
        name: 'admin-profile',
        component: () => import('../views/AdminProfileView.vue')
      }
    ]
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
