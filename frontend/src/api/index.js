import api, { unwrap } from './http'
import { getAccessToken } from './session'

export const authApi = {
  login: (data) => unwrap(api.post('/api/auth/login', data)),
  register: (data) => unwrap(api.post('/api/auth/register', data)),
  me: () => unwrap(api.get('/api/auth/me')),
  logout: (refreshToken) =>
    unwrap(api.post('/api/auth/logout', { refreshToken }))
}

export const categoryApi = {
  list: () => unwrap(api.get('/api/categories/tree'))
}

export const productApi = {
  page: (params) => unwrap(api.get('/api/products', { params })),
  detail: (id) => unwrap(api.get(`/api/products/${id}`)),
  reviews: (id, params) =>
    unwrap(api.get(`/api/products/${id}/reviews`, { params })),
  reviewSummary: (id) =>
    unwrap(api.get(`/api/products/${id}/reviews/summary`))
}

export const reviewApi = {
  create: (data) => unwrap(api.post('/api/reviews', data)),
  remove: (id) => unwrap(api.delete(`/api/reviews/${id}`))
}

export const cartApi = {
  list: () => unwrap(api.get('/api/carts')),
  add: (productId, num = 1) =>
    unwrap(api.post('/api/carts', { productId, num })),
  updateNum: (id, num) =>
    unwrap(api.put(`/api/carts/${id}/num`, { num })),
  remove: (id) => unwrap(api.delete(`/api/carts/${id}`)),
  clear: () => unwrap(api.delete('/api/carts/clear'))
}

export const orderApi = {
  page: (params) => unwrap(api.get('/api/orders', { params })),
  detail: (id) => unwrap(api.get(`/api/orders/${id}`)),
  create: (data) => unwrap(api.post('/api/orders', data)),
  pay: (id) => unwrap(api.put(`/api/orders/${id}/pay`)),
  cancel: (id) => unwrap(api.put(`/api/orders/${id}/cancel`)),
  confirm: (id) => unwrap(api.put(`/api/orders/${id}/confirm`))
}

export const sellerOrderApi = {
  page: (params) => unwrap(api.get('/api/seller/orders', { params })),
  detail: (id) => unwrap(api.get(`/api/seller/orders/${id}`)),
  ship: (id) => unwrap(api.put(`/api/seller/orders/${id}/ship`))
}

export const sellerProductApi = {
  page: (params) => unwrap(api.get('/api/seller/products', { params })),
  create: (data) => unwrap(api.post('/api/products', data)),
  update: (id, data) => unwrap(api.put(`/api/products/${id}`, data)),
  updateStatus: (id, status) =>
    unwrap(api.put(`/api/products/${id}/status/${status}`)),
  remove: (id) => unwrap(api.delete(`/api/products/${id}`))
}

export const returnApi = {
  apply: (data) => unwrap(api.post('/api/returns', data)),
  page: (params) => unwrap(api.get('/api/returns/my', { params })),
  cancel: (id) => unwrap(api.put(`/api/returns/${id}/cancel`))
}

export const sellerReturnApi = {
  page: (params) => unwrap(api.get('/api/seller/returns', { params })),
  approve: (id, note) =>
    unwrap(api.put(`/api/seller/returns/${id}/approve`, { note })),
  reject: (id, note) =>
    unwrap(api.put(`/api/seller/returns/${id}/reject`, { note }))
}

export const sellerReviewApi = {
  page: (params) => unwrap(api.get('/api/seller/reviews', { params })),
  reply: (id, content) =>
    unwrap(api.put(`/api/seller/reviews/${id}/reply`, { content }))
}

export const sellerStatsApi = {
  stats: (range = '30d') =>
    unwrap(api.get('/api/seller/stats', { params: { range } }))
}

export const adminUserApi = {
  buyers: (params) => unwrap(api.get('/api/admin/users/buyers', { params })),
  sellers: (params) => unwrap(api.get('/api/admin/users/sellers', { params })),
  removeBuyer: (id) => unwrap(api.delete(`/api/admin/users/buyers/${id}`)),
  removeSeller: (id) => unwrap(api.delete(`/api/admin/users/sellers/${id}`))
}

export const adminOrderApi = {
  page: (params) => unwrap(api.get('/api/admin/orders', { params })),
  detail: (id) => unwrap(api.get(`/api/admin/orders/${id}`)),
  forceCancel: (id) =>
    unwrap(api.put(`/api/admin/orders/${id}/force-cancel`))
}

export const adminCategoryApi = {
  page: (params) => unwrap(api.get('/api/admin/categories', { params })),
  create: (data) => unwrap(api.post('/api/categories', data)),
  update: (id, data) => unwrap(api.put(`/api/categories/${id}`, data)),
  updateStatus: (id, status) =>
    unwrap(api.put(`/api/categories/${id}/status/${status}`)),
  remove: (id) => unwrap(api.delete(`/api/categories/${id}`))
}

export const notificationApi = {
  page: (params) => unwrap(api.get('/api/notifications/my', { params })),
  markRead: (id) => unwrap(api.put(`/api/notifications/${id}/read`))
}

export const profileApi = {
  get: () => unwrap(api.get('/api/profile')),
  update: (data) => unwrap(api.put('/api/profile', data)),
  changePassword: (data) => unwrap(api.put('/api/profile/password', data))
}

export const uploadApi = {
  image: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return unwrap(
      api.post('/api/upload/image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    )
  }
}

export const agentApi = {
  chat: async (message) => {
    const response = await api.post(
      '/agent/chat',
      { message },
      { timeout: 240000 }
    )
    return response.data
  },
  streamChat: async (message, handlers = {}) => {
    const controller = new AbortController()
    const timeout = window.setTimeout(() => controller.abort(), 240000)
    try {
      const response = await fetch('/agent/chat/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${getAccessToken()}`
        },
        body: JSON.stringify({ message }),
        signal: controller.signal
      })
      if (!response.ok || !response.body) {
        throw new Error(response.status === 401 ? '登录状态已失效，请重新登录' : 'AI 服务暂不可用')
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      let finalData = null

      while (true) {
        const { done, value } = await reader.read()
        buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        for (const line of lines) {
          if (!line.trim()) continue
          const event = JSON.parse(line)
          if (event.type === 'status') handlers.onStatus?.(event.text)
          if (event.type === 'delta') handlers.onDelta?.(event.text)
          if (event.type === 'error') throw new Error(event.message || 'AI 服务暂不可用')
          if (event.type === 'done') finalData = event.data
        }
        if (done) break
      }
      if (buffer.trim()) {
        const event = JSON.parse(buffer)
        if (event.type === 'delta') handlers.onDelta?.(event.text)
        if (event.type === 'error') throw new Error(event.message || 'AI 服务暂不可用')
        if (event.type === 'done') finalData = event.data
      }
      return finalData || {}
    } finally {
      window.clearTimeout(timeout)
    }
  }
}
