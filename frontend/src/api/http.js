import axios from 'axios'
import {
  clearSession,
  getAccessToken,
  getRefreshToken,
  saveSession
} from './session'
import { localizeMessage } from '../utils/message'

const api = axios.create({
  baseURL: '',
  timeout: 30000
})

let refreshTask = null

api.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const responseMessage =
      error.response?.data?.message || error.response?.data?.detail
    if (responseMessage) {
      error.response.data.message = localizeMessage(responseMessage)
      error.response.data.detail = localizeMessage(responseMessage)
    }
    if (error.message) {
      error.message = localizeMessage(error.message)
    }

    const original = error.config
    const status = error.response?.status
    const refreshToken = getRefreshToken()
    const isAuthEndpoint = original?.url?.startsWith('/api/auth/')

    if (status !== 401 || original?._retry || isAuthEndpoint || !refreshToken) {
      return Promise.reject(error)
    }

    original._retry = true
    try {
      refreshTask ||= axios
        .post('/api/auth/refresh', { refreshToken })
        .then((response) => {
          const body = response.data
          if (body.code !== 200) {
            throw new Error(localizeMessage(body.message) || '登录已过期')
          }
          saveSession(body.data)
          return body.data.token
        })
        .finally(() => {
          refreshTask = null
        })

      const token = await refreshTask
      original.headers.Authorization = `Bearer ${token}`
      return api(original)
    } catch (refreshError) {
      clearSession()
      if (window.location.pathname !== '/login') {
        window.location.href = `/login?redirect=${encodeURIComponent(
          window.location.pathname + window.location.search
        )}`
      }
      return Promise.reject(refreshError)
    }
  }
)

export async function unwrap(request) {
  const response = await request
  const body = response.data
  if (!body || body.code !== 200) {
    throw new Error(localizeMessage(body?.message) || '请求失败')
  }
  return body.data
}

export default api
