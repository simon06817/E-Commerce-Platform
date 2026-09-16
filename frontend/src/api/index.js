import api, { unwrap } from './http'

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

export const returnApi = {
  apply: (data) => unwrap(api.post('/api/returns', data)),
  page: (params) => unwrap(api.get('/api/returns/my', { params })),
  cancel: (id) => unwrap(api.put(`/api/returns/${id}/cancel`))
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
    const response = await api.post('/agent/chat', { message })
    return response.data
  }
}
