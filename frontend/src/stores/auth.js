import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '../api'
import {
  clearSession,
  getAvatarUrl,
  getSession,
  roleHome,
  saveSession,
  setAvatarUrl
} from '../api/session'
import { useAssistantStore } from './assistant'

export const useAuthStore = defineStore('auth', () => {
  const session = ref(getSession())

  const token = computed(() => session.value?.token || '')
  const refreshToken = computed(() => session.value?.refreshToken || '')
  const userId = computed(() => session.value?.userId || null)
  const role = computed(() => session.value?.role || '')
  const username = computed(() => session.value?.username || '')
  const displayName = computed(
    () => session.value?.displayName || session.value?.username || '用户'
  )
  const isLoggedIn = computed(() => Boolean(token.value))
  const avatarUrl = computed(() => getAvatarUrl(userId.value))
  const defaultRoute = computed(() => roleHome(role.value))

  function applySession(data) {
    session.value = saveSession(data)
  }

  async function login(payload) {
    const data = await authApi.login(payload)
    applySession(data)
    return data
  }

  async function register(payload) {
    const data = await authApi.register(payload)
    applySession(data)
    return data
  }

  async function logout() {
    try {
      await authApi.logout(refreshToken.value)
    } finally {
      clearSession()
      session.value = null
      useAssistantStore().clearAll()
    }
  }

  function setAvatar(url) {
    setAvatarUrl(userId.value, url)
  }

  function updateDisplayName(displayName) {
    if (!session.value || !displayName) return
    applySession({
      ...session.value,
      displayName
    })
  }

  return {
    session,
    token,
    refreshToken,
    userId,
    role,
    username,
    displayName,
    isLoggedIn,
    avatarUrl,
    defaultRoute,
    login,
    register,
    logout,
    setAvatar,
    updateDisplayName
  }
})
